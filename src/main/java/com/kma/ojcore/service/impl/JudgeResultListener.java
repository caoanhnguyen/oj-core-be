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
import com.kma.ojcore.repository.*;
import com.kma.ojcore.service.scoring.ContestScoringStrategy;
import com.kma.ojcore.service.scoring.ScoringStrategyFactory;
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
    private final ScoringStrategyFactory scoringStrategyFactory;
    private final ContestParticipationRepository contestParticipationRepository;
    private final ContestProblemRepository contestProblemRepository;

    // ========================================================
    // FLOW 1: PROCESS SUBMISSION JUDGE RESULT
    // ========================================================
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional
    public void handleJudgeResult(JudgeResultSdi result) {
        log.info("Received judge result from RabbitMQ for Submission ID: [{}] - Verdict: {}", result.getSubmissionId(), result.getSubmissionVerdict());

        Submission submission = submissionRepository.findById(result.getSubmissionId()).orElse(null);
        if (submission == null) {
            log.error("Submission [{}] not found in database!", result.getSubmissionId());
            return;
        }

        // Get User, Problem and calculate final scale score if this is an OI Contest
        User user = submission.getUser();
        Problem problem = submission.getProblem();
        
        Integer finalScore = result.getScore();
        if (submission.getContest() != null && submission.getContest().getRuleType() == RuleType.OI) {
            Integer contestPoints = contestProblemRepository.findPointsByContestIdAndProblemId(
                    submission.getContest().getId(), problem.getId()
            );

            if (contestPoints != null) {
                double rawScore = result.getScore() != null ? result.getScore().doubleValue() : 0.0;
                double baseScore = problem.getTotalScore() != null ? problem.getTotalScore().doubleValue() : 100.0;
                finalScore = (int) Math.round((rawScore / baseScore) * contestPoints);
            }
        }

        // Update Submission
        submission.setVerdict(result.getSubmissionVerdict());
        submission.setScore(finalScore); 
        submission.setPassedTestCount(result.getPassedTestCount());
        submission.setTotalTestCount(result.getTotalTestCount());
        submission.setExecutionTimeMs(result.getExecutionTimeMs());
        submission.setExecutionMemoryMb(result.getExecutionMemoryMb());
        submission.setErrorMessage(result.getErrorMessage());

        // Save submission status early
        // Ensure Admin submission history is saved even if the flow is interrupted below
        submission.setSubmissionStatus(result.getSubmissionStatus());
        submissionRepository.save(submission);

        if (user != null && problem != null) {

            // 1. Check Role (Ghost Mode mechanism)
            boolean isStaff = user.getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN") || r.getName().name().equals("ROLE_MODERATOR"));

            if (isStaff) {
                log.info("Staff debug mode: Saved test result, SKIPPING points and ranking update for Submission [{}]", submission.getId());
                return; // Interrupt flow here! Do not execute OI/ACM logic below.
            }

            // 2. ALWAYS INCREMENT SUBMISSION_COUNT FOR USER AND PROBLEM
            int currentUserSub = user.getSubmissionCount() != null ? user.getSubmissionCount() : 0;
            user.setSubmissionCount(currentUserSub + 1);

            long currentProbSub = problem.getSubmissionCount() != null ? problem.getSubmissionCount() : 0L;
            problem.setSubmissionCount(currentProbSub + 1L);

            boolean isAc = "AC".equals(result.getSubmissionVerdict().toString());

            // 3. IF AC -> INCREMENT AC_COUNT FOR USER AND PROBLEM
            if (isAc) {
                int currentUserAc = user.getAcCount() != null ? user.getAcCount() : 0;
                user.setAcCount(currentUserAc + 1);

                long currentProbAc = problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0L;
                problem.setAcceptedCount(currentProbAc + 1L);
            }

            // =========================================================
            // 4. PROTECT SOLVED_COUNT AND CALCULATE SCORE (STATUS TABLE)
            // =========================================================
            UserProblemStatus status = userProblemStatusRepo
                    .findByUserIdAndProblemId(user.getId(), problem.getId())
                    .orElse(UserProblemStatus.builder()
                            .user(user)
                            .problem(problem)
                            .state(UserProblemState.ATTEMPTED)
                            .maxScore(0.0)
                            .build());

            // SPLIT LOGIC: ACM and OI
            if (problem.getRuleType() == RuleType.ACM) {
                // ACM LOGIC
                if (isAc) {
                    if (status.getState() != UserProblemState.SOLVED) {
                        status.setState(UserProblemState.SOLVED);

                        // Increment Solved Count
                        int currentSolved = user.getSolvedCount() != null ? user.getSolvedCount() : 0;
                        user.setSolvedCount(currentSolved + 1);
                    }
                } else if (status.getState() != UserProblemState.SOLVED) {
                    status.setState(UserProblemState.ATTEMPTED);
                }
            } else {
                // =============== OI LOGIC ===============
                double currentScore = result.getScore() != null ? result.getScore().doubleValue() : 0.0;
                double previousMax = status.getMaxScore() != null ? status.getMaxScore() : 0.0;

                // 4.1 Update Max Score (totalScore)
                if (currentScore > previousMax) {
                    double scoreDiff = currentScore - previousMax;
                    status.setMaxScore(currentScore);

                    double userTotalScore = user.getTotalScore() != null ? user.getTotalScore() : 0.0;
                    user.setTotalScore(userTotalScore + scoreDiff);
                }

                // 4.2 Update Problem Status & Solved Count
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

            // 5. SAVE ALL TO DB AT ONCE (Performance optimization)
            userRepository.save(user);
            problemRepository.save(problem);
            userProblemStatusRepo.save(status);

            // =======================================================
            // CONTEST SCORING ENGINE
            // =======================================================
            if (submission.getContest() != null) {
                // Kiểm tra Upsolving: Nộp sau khi kết thúc kỳ thi -> Không tính điểm!
                if (!submission.getCreatedDate().isAfter(submission.getContest().getEndTime())) {

                    contestParticipationRepository.findByContestIdAndUserId(
                                    submission.getContest().getId(), submission.getUser().getId())
                            .ifPresent(participation -> {

                                // Gọi Strategy dựa theo RuleType của Contest
                                ContestScoringStrategy strategy = scoringStrategyFactory.getStrategy(submission.getContest().getRuleType());
                                strategy.processScore(submission, participation);

                                // Lưu lại điểm số mới vào DB
                                contestParticipationRepository.save(participation);
                                log.info("Successfully updated leaderboard participation for user: {}", user.getUsername());
                            });
                } else {
                    log.info("Submission {} is Upsolving (submitted after contest ended). Score not counted.", submission.getId());
                }
            }
        }
    }

    // ========================================================
    // FLOW 2: PROCESS RUN CODE RESULT
    // ========================================================
    @RabbitListener(queues = RabbitMQConfig.RUN_CODE_RESULT_QUEUE)
    public void handleRunCodeResult(RunCodeResponse response) {
        try {
            log.info("Received Run Code result from RabbitMQ. Token: [{}]", response.getRunToken());

            String redisKey = "RUN_CODE_RESULT:" + response.getRunToken();
            String jsonValue = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(redisKey, jsonValue, 5, TimeUnit.MINUTES);
            log.info("Successfully saved Run Code result to Redis. Ready for Frontend fetching.");

        } catch (Exception e) {
            log.error("Critical error while saving Run Code result...", e);
        }
    }
}