package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeSdi;
import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ResourceNotFoundException;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final LanguageLoader languageLoader;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UUID submitCode(SubmissionSdi request, UUID currentUserId) {

        // 1 & 2: Tìm Problem và Validate
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy bài toán!"));

        if (!problem.getAllowedLanguages().contains(request.getLanguageKey())) {
            throw new BusinessException("Ngôn ngữ " + request.getLanguageKey() + " không được hỗ trợ!");
        }

        // Lấy cấu hình ngôn ngữ từ LanguageLoader
        LanguageConfig langConfig = languageLoader.getConfigByKey(request.getLanguageKey());
        if (langConfig == null) {
            throw new BusinessException("Hệ thống chưa cấu hình ngôn ngữ này!");
        }

        User user = userRepository.getReferenceById(currentUserId);

        // 3 & 4: Khởi tạo Submission và Lưu DB
        Submission submission = Submission.builder()
                .problem(problem)
                .user(user)
                .languageKey(request.getLanguageKey())
                .sourceCode(request.getSourceCode())
                .submissionStatus(SubmissionStatus.PENDING)
                .verdict(SubmissionVerdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        // Cộng lượt nộp vào Problem
        problemRepository.incrementSubmissionCount(problem.getId());

        // --- 5. TÍNH TOÁN FINAL LIMIT VÀ ĐÓNG GÓI JUDGE SDI ---
        // Công thức: Final = (Base * Multiplier) + Allowance
        int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier()) + langConfig.getTimeLimitAllowance();
        int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier()) + langConfig.getMemoryLimitAllowance();

        JudgeSdi sdi = JudgeSdi.builder()
                .submissionId(submission.getId())
                .problemId(problem.getId())
                .ruleType(problem.getRuleType().name()) // Bắn chữ "ACM" hoặc "OI" sang cho máy chấm
                .sourceCode(request.getSourceCode())
                .languageKey(request.getLanguageKey())
                .compileCommand(langConfig.getCompileCommand())
                .runCommand(langConfig.getRunCommand())
                .isCompiled(langConfig.isCompiled())
                .sourceName(langConfig.getSourceName())
                .exeName(langConfig.getExeName())
                .finalTimeLimitMs(finalTimeLimit)
                .finalMemoryLimitMb(finalMemoryLimit)
                .build();

        // 6. PHÓNG VÀO RABBITMQ
        // Bọc lệnh gửi MQ vào AfterCommit. Về cơ bản là nó sẽ đợi đến khi transaction DB hoàn tất và commit thành công
        // thì mới thực sự gửi message đi. Nếu trong quá trình xử lý có lỗi và transaction bị rollback,
        // thì lệnh gửi MQ sẽ không được thực thi, tránh tình trạng "điếc không sợ súng" gửi đi một cái submissionId
        // mà sau đó lại rollback DB thì máy chấm sẽ không tìm thấy submission đó để chấm.
        Submission finalSubmission = submission;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.JUDGE_ROUTING_KEY, sdi);
                log.info("Đã ném Submission [{}] vào RabbitMQ SAU KHI DB ĐÃ COMMIT!", finalSubmission.getId());
            }
        });

        return submission.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public SubmissionDetailsSdo getSubmissionBasicInfo(UUID submissionId) {
        if(!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Không tìm thấy submission với ID: " + submissionId);
        }

        return submissionRepository.getDetails(submissionId);
    }

    @Override
    public RunCodeResponse getRunCodeResult(UUID token) {
        String redisKey = "RUN_CODE_RESULT:" + token.toString();

        // Chọc vào Redis tìm xem có đồ không
        String jsonResult = redisTemplate.opsForValue().get(redisKey);

        if (jsonResult != null) {
            try {
                // Parse lại chuỗi JSON thành Object để Spring Boot trả về dạng JSON chuẩn
                return objectMapper.readValue(jsonResult, RunCodeResponse.class);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý kết quả Run Code từ Redis", e);
                throw new BusinessException("Lỗi khi xử lý kết quả Run Code");
            }
        } else {
            log.info("Kết quả đang được xử lý hoặc đã bị xóa khỏi REDIS. Token: [{}]", token);
            throw new BusinessException("Kết quả Run Code vẫn đang được xử lý. Vui lòng thử lại sau vài giây.");
        }
    }

    @Override
    public Page<?> getSubmissions(UUID problemId,
                                  UUID userId,
                                  SubmissionVerdict submissionVerdict,
                                  String username,
                                  EStatus status,
                                  ProblemStatus problemStatus,
                                  List<SubmissionVerdict> allowedVerdicts,
                                  boolean hideStaff,
                                  Pageable pageable) {
        // Validate problem và user
        if (problemId != null && !problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Không tìm thấy bài toán với ID: " + problemId);
        }

        if (userId != null && !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }

        return submissionRepository.getSubmissions(problemId, userId, submissionVerdict, username, status, problemStatus, allowedVerdicts, hideStaff, pageable);
    }

    @Override
    public ProblemStatisticSdo getProblemStatistics(UUID problemId, List<SubmissionVerdict> allowedVerdicts) {
        List<SubmissionRepository.VerdictCountProjection> projections =
                submissionRepository.countSubmissionsByVerdict(problemId);

        long total = 0;
        Map<String, Long> counts = new HashMap<>();

        // 1. Khởi tạo sẵn giá trị 0 dựa trên danh sách Controller cho phép
        for (SubmissionVerdict v : allowedVerdicts) {
            counts.put(v.name(), 0L);
        }

        // 2. Lặp kết quả từ DB và cộng dồn
        for (SubmissionRepository.VerdictCountProjection p : projections) {
            SubmissionVerdict verdict = SubmissionVerdict.valueOf(p.getVerdict()); // Trả về kiểu Enum

            // Nếu DB trả ra cái verdict KHÔNG NẰM TRONG danh sách cho phép -> Bỏ qua!
            if (!allowedVerdicts.contains(verdict)) {
                continue;
            }

            long count = p.getCount();
            counts.put(verdict.name(), count);
            total += count; // Chỉ lấy tổng của những lần nộp được phép
        }

        return ProblemStatisticSdo.builder()
                .totalSubmissions(total)
                .verdictCounts(counts)
                .build();
    }

    @Override
    public String getLatestSubmissionCode(UUID problemId, UUID userId, String languageKey) {
        return submissionRepository.findFirstSourceCodeByProblemIdAndUserIdAndLanguageKey(problemId, userId, languageKey).orElse(null);
    }
}