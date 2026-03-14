package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeResultSdi;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeResultListener {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Cái tai nghe gắn chặt vào RESULT_QUEUE
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional(rollbackFor = Throwable.class)
    public void handleJudgeResult(JudgeResultSdi result) {
        try {
            log.info("Nhận được kết quả chấm bài từ Judge Service cho Submission [{}]", result.getSubmissionId());

            // 1. Tìm lại bài nộp trong DB
            Submission submission = submissionRepository.findById(result.getSubmissionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Submission ID: " + result.getSubmissionId()));

            // 2. Cập nhật mọi thông số máy chấm trả về
            submission.setSubmissionStatus(result.getSubmissionStatus());
            submission.setVerdict(result.getSubmissionVerdict());
            submission.setScore(result.getScore());
            submission.setPassedTestCount(result.getPassedTestCount());
            submission.setTotalTestCount(result.getTotalTestCount());
            submission.setExecutionTimeMs(result.getExecutionTimeMs());
            submission.setExecutionMemoryMb(result.getExecutionMemoryMb());
            submission.setErrorMessage(result.getErrorMessage());

            submissionRepository.save(submission);

            // 3. Nếu kết quả là AC (Accepted), cộng thêm 1 lượt giải đúng cho Problem
            if (result.getSubmissionVerdict() == SubmissionVerdict.AC) {
                Problem problem = submission.getProblem();
                problem.setAcceptedCount(problem.getAcceptedCount() + 1L);
                problemRepository.save(problem);
                log.info("Submission [{}] AC! Đã cộng điểm cho Problem [{}]", submission.getId(), problem.getId());
            } else {
                log.info("Submission [{}] Verdict: {}", submission.getId(), result.getSubmissionVerdict());
            }
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi xử lý kết quả Submission: {}", e.getMessage(), e);
        }

    }

    // ========================================================
    // LUỒNG 2: XỬ LÝ KẾT QUẢ CHẠY THỬ (RUN CODE) - MỚI THÊM
    // ========================================================
    @RabbitListener(queues = RabbitMQConfig.RUN_CODE_RESULT_QUEUE)
    public void handleRunCodeResult(RunCodeResponse response) {
        try {
            log.info("Đã nhận kết quả Run Code từ RabbitMQ. Token: [{}]", response.getRunToken());

            String redisKey = "RUN_CODE_RESULT:" + response.getRunToken();

            // Biến Object thành chuỗi JSON để lưu vào Redis
            String jsonValue = objectMapper.writeValueAsString(response);

            // Lưu vào Redis, cho nó sống đúng 5 phút (TTL). Quá 5 phút tự bốc hơi dọn rác.
            redisTemplate.opsForValue().set(redisKey, jsonValue, 5, TimeUnit.MINUTES);

            log.info("Đã lưu kết quả Run Code vào Redis thành công. Sẵn sàng cho Frontend lấy!");

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi lưu kết quả Run Code vào Redis. Token [{}]: {}", response.getRunToken(), e.getMessage(), e);
        }
    }
}