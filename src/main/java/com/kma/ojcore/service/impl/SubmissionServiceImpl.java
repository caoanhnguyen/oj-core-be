package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeSdi;
import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    // Inject thêm 2 vũ khí hạng nặng
    private final LanguageLoader languageLoader;
    private final RabbitTemplate rabbitTemplate;

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

        problem.setSubmissionCount(problem.getSubmissionCount() + 1L);
        problemRepository.save(problem);

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
                rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_QUEUE, sdi);
                log.info("Đã ném Submission [{}] vào RabbitMQ SAU KHI DB ĐÃ COMMIT!", finalSubmission.getId());
            }
        });

        return submission.getId();
    }
}