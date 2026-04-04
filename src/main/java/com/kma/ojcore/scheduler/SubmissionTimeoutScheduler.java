package com.kma.ojcore.scheduler;

import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeResultSdi;
import com.kma.ojcore.dto.request.submissions.JudgeSdi;
import com.kma.ojcore.entity.LanguageConfig;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionTimeoutScheduler {

    private final SubmissionRepository submissionRepository;
    private final LanguageLoader languageLoader;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Throwable.class)
    @Scheduled(cron = "0 */5 * * * *") // Chạy mỗi 5 phút
    public void rescueStuckSubmissions() {
        log.info("[Submission Timeout Scheduler] - Bắt đầu kiểm tra các submisison bị kẹt...");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);
        List<Submission> stuckSubmissions = submissionRepository.findStuckSubmissions(timeoutThreshold);

        if (stuckSubmissions.isEmpty()) {
            log.info("Không tìm thấy submission nào bị kẹt.");
            return;
        }

        log.warn("Tìm thấy {} submission bị kẹt. Đang cập nhật trạng thái...", stuckSubmissions.size());

        for (Submission submission : stuckSubmissions) {
            int currentRetry = submission.getRetryCount() != null ? submission.getRetryCount() : 0;

            if (currentRetry >= 3) {
                log.error("Submission [{}] đã retry 3 lần nhưng vẫn kẹt. Bắn kết quả System Error về hệ thống...", submission.getId());
                
                JudgeResultSdi failSdi = com.kma.ojcore.dto.request.submissions.JudgeResultSdi.builder()
                        .submissionId(submission.getId())
                        .submissionStatus(SubmissionStatus.FAILED)
                        .submissionVerdict(SubmissionVerdict.SE)
                        .errorMessage("Lỗi hệ thống: Máy chấm không phản hồi quá lâu. Vui lòng liên hệ Admin.")
                        .build();

                rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.RESULT_ROUTING_KEY, failSdi);
            } else {
                // Tăng biến đếm và reset thời gian trước khi build lại
                submission.setRetryCount(currentRetry + 1);
                submission.setUpdatedDate(LocalDateTime.now());
                submissionRepository.save(submission);

                try {
                    Problem problem = submission.getProblem();
                    LanguageConfig langConfig = languageLoader.getConfigByKey(submission.getLanguageKey());

                    if (langConfig == null) {
                        throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
                    }

                    // Tính toán lại limit chuẩn xác
                    int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier()) + langConfig.getTimeLimitAllowance();
                    int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier()) + langConfig.getMemoryLimitAllowance();

                    JudgeSdi sdi = JudgeSdi.builder()
                            .submissionId(submission.getId())
                            .problemId(problem.getId())
                            .ruleType(submission.getContest() != null ? submission.getContest().getRuleType().name() : problem.getRuleType().name())
                            .sourceCode(submission.getSourceCode())
                            .languageKey(submission.getLanguageKey())
                            .compileCommand(langConfig.getCompileCommand())
                            .runCommand(langConfig.getRunCommand())
                            .isCompiled(langConfig.isCompiled())
                            .sourceName(langConfig.getSourceName())
                            .exeName(langConfig.getExeName())
                            .finalTimeLimitMs(finalTimeLimit)
                            .finalMemoryLimitMb(finalMemoryLimit)
                            .build();

                    final int nextRetryAttempt = currentRetry + 1;
                    final UUID currentSubmissionId = submission.getId();

                    // Bắn lại vào Exchange chuẩn chỉ
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.JUDGE_ROUTING_KEY, sdi);
                            log.info("Đã đẩy lại Submission [{}] vào RabbitMQ (Lần {}) SAU KHI DB COMMIT!", currentSubmissionId, nextRetryAttempt);
                        }
                    });
                } catch (Exception e) {
                    log.error("Lỗi khi build JudgeSdi cho submission {}: {}", submission.getId(), e.getMessage());
                }
            }
        }
        log.info("[Submission Timeout Scheduler] Hoàn tất công tác cứu hộ.");
    }
}
