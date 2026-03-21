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

        // Đưa việc lưu trạng thái Submission lên đây
        // Đảm bảo Admin vẫn lưu được lịch sử nộp bài dù có bị ngắt luồng phía dưới
        submission.setSubmissionStatus(result.getSubmissionStatus());
        submissionRepository.save(submission);

        // Lấy User và Problem
        User user = submission.getUser();
        Problem problem = submission.getProblem();

        if (user != null && problem != null) {

            // 1. Kiểm tra Role (Cơ chế Ghost Mode chuẩn QDUOJ)
            boolean isStaff = user.getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN") || r.getName().name().equals("ROLE_MODERATOR"));

            if (isStaff) {
                log.info("Staff debug mode: Đã lưu kết quả test đề, BỎ QUA cộng điểm và Ranking cho Submission [{}]", submission.getId());
                return; // Ngắt luồng tại đây! Không chạy xuống logic OI/ACM bên dưới.
            }

            // 2. LUÔN CỘNG SUBMISSION_COUNT CHO USER VÀ PROBLEM
            int currentUserSub = user.getSubmissionCount() != null ? user.getSubmissionCount() : 0;
            user.setSubmissionCount(currentUserSub + 1);

            long currentProbSub = problem.getSubmissionCount() != null ? problem.getSubmissionCount() : 0L;
            problem.setSubmissionCount(currentProbSub + 1L);

            boolean isAc = "AC".equals(result.getSubmissionVerdict().toString());

            // 3. NẾU AC -> CỘNG AC_COUNT CHO USER VÀ PROBLEM (ĐẾM MÙ)
            if (isAc) {
                int currentUserAc = user.getAcCount() != null ? user.getAcCount() : 0;
                user.setAcCount(currentUserAc + 1);

                long currentProbAc = problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0L;
                problem.setAcceptedCount(currentProbAc + 1L);
            }

            // =========================================================
            // 4. LOGIC BẢO VỆ SOLVED_COUNT VÀ TÍNH ĐIỂM (BẢNG STATUS)
            // =========================================================
            UserProblemStatus status = userProblemStatusRepo
                    .findByUserIdAndProblemId(user.getId(), problem.getId())
                    .orElse(UserProblemStatus.builder()
                            .user(user)
                            .problem(problem)
                            .state(UserProblemState.ATTEMPTED)
                            .maxScore(0.0)
                            .build());

            // CHẺ NHÁNH LOGIC: ACM và OI
            if (problem.getRuleType() == RuleType.ACM) {
                // LOGIC ACM
                if (isAc) {
                    if (status.getState() != UserProblemState.SOLVED) {
                        status.setState(UserProblemState.SOLVED);

                        // Cộng số BÀI TẬP ĐÃ GIẢI (Solved Count)
                        int currentSolved = user.getSolvedCount() != null ? user.getSolvedCount() : 0;
                        user.setSolvedCount(currentSolved + 1);
                    }
                } else if (status.getState() != UserProblemState.SOLVED) {
                    status.setState(UserProblemState.ATTEMPTED);
                }
            } else {
                // =============== LOGIC OI ===============
                double currentScore = result.getScore() != null ? result.getScore().doubleValue() : 0.0;
                double previousMax = status.getMaxScore() != null ? status.getMaxScore() : 0.0;

                // 4.1 Cập nhật Kỷ lục điểm (totalScore)
                if (currentScore > previousMax) {
                    double scoreDiff = currentScore - previousMax;
                    status.setMaxScore(currentScore);

                    double userTotalScore = user.getTotalScore() != null ? user.getTotalScore() : 0.0;
                    user.setTotalScore(userTotalScore + scoreDiff);
                }

                // 4.2 Cập nhật Trạng thái bài làm & Solved Count
                double problemTotalScore = problem.getTotalScore() != null ? problem.getTotalScore().doubleValue() : 0.0;

                if (isAc || currentScore >= problemTotalScore) {
                    if (status.getState() != UserProblemState.SOLVED) {
                        status.setState(UserProblemState.SOLVED);

                        int currentSolved = user.getSolvedCount() != null ? user.getSolvedCount() : 0;
                        user.setSolvedCount(currentSolved + 1);
                    }
                } else if (status.getState() != UserProblemState.SOLVED) {
                    status.setState(UserProblemState.ATTEMPTED);
                }
            }

            // 5. LƯU TẤT CẢ VÀO DB CÙNG 1 LÚC (Tối ưu hiệu năng)
            userRepository.save(user);
            problemRepository.save(problem);
            userProblemStatusRepo.save(status);
        }
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