package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeResultSdi;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.entity.UserProblemStatus;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.enums.UserProblemState;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import com.kma.ojcore.repository.UserProblemStatusRepository;
import com.kma.ojcore.repository.UserRepository;
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
    private final UserProblemStatusRepository userProblemStatusRepo;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ========================================================
    // LUỒNG 1: XỬ LÝ KẾT QUẢ CHẤM SUBMIT
    // ========================================================
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional
    public void handleJudgeResult(JudgeResultSdi result) {
        log.info("Đã nhận kết quả chấm từ RabbitMQ cho Submission ID: [{}] - Verdict: {}", result.getSubmissionId(), result.getSubmissionVerdict());

        Submission submission = submissionRepository.findById(result.getSubmissionId()).orElse(null);
        if (submission == null) {
            log.error("Không tìm thấy Submission [{}] trong DB!", result.getSubmissionId());
            return;
        }

        // Cập nhật Submission
        submission.setVerdict(result.getSubmissionVerdict());
        submission.setScore(result.getScore());
        submission.setPassedTestCount(result.getPassedTestCount());
        submission.setTotalTestCount(result.getTotalTestCount());
        submission.setExecutionTimeMs(result.getExecutionTimeMs());
        submission.setExecutionMemoryMb(result.getExecutionMemoryMb());
        submission.setErrorMessage(result.getErrorMessage());

        // Lấy User và Problem
        User user = submission.getUser();
        Problem problem = submission.getProblem();

        if (user != null && problem != null) {
            UserProblemStatus status = userProblemStatusRepo
                    .findByUserIdAndProblemId(user.getId(), problem.getId())
                    .orElse(UserProblemStatus.builder()
                            .user(user)
                            .problem(problem)
                            .state(UserProblemState.ATTEMPTED)
                            .maxScore(0.0)
                            .build());

            boolean isAc = "AC".equals(result.getSubmissionVerdict().toString());

            // CHẺ NHÁNH LOGIC: ACM và OI
            if (problem.getRuleType() == RuleType.ACM) {
                // LOGIC ACM: Sử dụng solvedCount có sẵn
                if (isAc) {
                    if (status.getState() != UserProblemState.SOLVED) {
                        status.setState(UserProblemState.SOLVED);

                        int currentSolved = user.getSolvedCount() != null ? user.getSolvedCount() : 0;
                        user.setSolvedCount(currentSolved + 1);
                        userRepository.save(user);
                    }
                } else if (status.getState() != UserProblemState.SOLVED) {
                    status.setState(UserProblemState.ATTEMPTED);
                }
            } else {
                // LOGIC OI: Cập nhật điểm số nếu cao hơn, và cập nhật trạng thái dựa trên max score hoặc AC
                double currentScore = result.getScore() != null ? result.getScore().doubleValue() : 0.0;
                double previousMax = status.getMaxScore() != null ? status.getMaxScore() : 0.0;

                if (currentScore > previousMax) {
                    double scoreDiff = currentScore - previousMax;
                    status.setMaxScore(currentScore);

                    double userTotalScore = user.getTotalScore() != null ? user.getTotalScore() : 0.0;
                    user.setTotalScore(userTotalScore + scoreDiff);
                    userRepository.save(user);
                }

                // Cập nhật state (Đạt max điểm của bài hoặc Verdict AC)
                double problemTotalScore = problem.getTotalScore() != null ? problem.getTotalScore().doubleValue() : 0.0;
                if (isAc || currentScore >= problemTotalScore) {
                    status.setState(UserProblemState.SOLVED);
                } else if (status.getState() != UserProblemState.SOLVED) {
                    status.setState(UserProblemState.ATTEMPTED);
                }
            }

            userProblemStatusRepo.save(status);
        }

        // Lưu trạng thái cuối cùng
        submission.setSubmissionStatus(result.getSubmissionStatus());
        submissionRepository.save(submission);
    }

    // ========================================================
    // LUỒNG 2: XỬ LÝ KẾT QUẢ CHẠY THỬ (RUN CODE)
    // ========================================================
    @RabbitListener(queues = RabbitMQConfig.RUN_CODE_RESULT_QUEUE)
    public void handleRunCodeResult(RunCodeResponse response) {
        try {
            log.info("Đã nhận kết quả Run Code từ RabbitMQ. Token: [{}]", response.getRunToken());

            String redisKey = "RUN_CODE_RESULT:" + response.getRunToken();
            String jsonValue = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(redisKey, jsonValue, 5, TimeUnit.MINUTES);
            log.info("Đã lưu kết quả Run Code vào Redis thành công. Sẵn sàng cho Frontend lấy!");

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi lưu kết quả Run Code...", e);
        }
    }
}