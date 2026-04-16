package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeSdi;
import com.kma.ojcore.dto.request.submissions.RejudgeSdi;
import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
import com.kma.ojcore.dto.response.submissions.SubmissionStatusSdo;
import com.kma.ojcore.entity.*;
import com.kma.ojcore.enums.*;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.ContestMapper;
import com.kma.ojcore.repository.*;
import com.kma.ojcore.service.SubmissionService;
import com.kma.ojcore.utils.EscapeHelper;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final UserProblemStatusRepository userProblemStatusRepository;
    private final LanguageLoader languageLoader;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ContestRepository contestRepository;
    private final ContestParticipationRepository contestParticipationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestMapper contestMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UUID submitCode(SubmissionSdi request, UUID currentUserId) {

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));


        if (!problem.getAllowedLanguages().contains(request.getLanguageKey())) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }

        LanguageConfig langConfig = languageLoader.getConfigByKey(request.getLanguageKey());
        if (langConfig == null) {
            throw new BusinessException(ErrorCode.LANGUAGE_NOT_SUPPORTED);
        }

        Contest contest = null;
        if (request.getContestId() != null) {
            contest = contestRepository.findByIdAndStatus(request.getContestId(), EStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND,
                            "Contest not found or not active."));

            // Luật 1: Cấm thi khi chưa mở cổng
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus == ContestStatus.UPCOMING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Contest has not started yet. You cannot submit solutions at this time.");
            }

            // Luật 1b: Cấm nộp khi contest đã kết thúc VÀ tài nguyên bị khóa (ONLY_DURING)
            if (timeStatus == ContestStatus.ENDED) {
                com.kma.ojcore.enums.ContestResourceVisibility rv = contest.getResourceVisibility();
                if (rv == com.kma.ojcore.enums.ContestResourceVisibility.ONLY_DURING) {
                    throw new BusinessException(ErrorCode.RESOURCE_ACCESS_DENIED,
                            "Contest has ended. You cannot submit solutions at this time.");
                }
                // ALWAYS_VISIBLE → Cho phép upsolving
            }

            // Luật 2: Lấy Participation lên để check đăng ký và quyền thi đấu cá nhân
            ContestParticipation participation = contestParticipationRepository
                    .findByContestIdAndUserId(contest.getId(), currentUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_REGISTERED,
                            "You are not registered for this contest."));

            if (participation.getIsDisqualified()) {
                throw new BusinessException(ErrorCode.BANNED_FROM_CONTEST, "You are disqualified from this contest.");
            }

            // ==========================================
            // THÊM CHỐT CHẶN PHIÊN THI CÁ NHÂN (DMOJ)
            // ==========================================
            // 1. Chỉ chặn chưa bấm Start NẾU ĐÂY LÀ WINDOWED CONTEST (có duration)
            if (participation.getStartTime() == null) {
                if (contest.getDurationMinutes() != null && contest.getDurationMinutes() > 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "You must start the contest before submitting.");
                }
            }
            // 2. Chặn nếu đã bị ép kết thúc
            if (participation.getIsFinished()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You have already finished this contest.");
            }

            // 3. Tự động tước quyền nếu Hết giờ cá nhân (CHỈ CHECK KHI END TIME KHÔNG NULL)
            if (participation.getEndTime() != null
                    && java.time.LocalDateTime.now().isAfter(participation.getEndTime())) {
                participation.setIsFinished(true); // Tự động khóa mõm luôn
                contestParticipationRepository.save(participation);
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Your contest session time has expired.");
            }
            // ==========================================

            // Luật 3: Bài toán này có nằm trong Contest không?
            if (!contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "This problem does not belong to the requested contest.");
            }
        }

        User user = userRepository.getReferenceById(currentUserId);

        Submission submission = Submission.builder()
                .problem(problem)
                .user(user)
                .contest(contest)
                .languageKey(request.getLanguageKey())
                .sourceCode(request.getSourceCode())
                .submissionStatus(SubmissionStatus.PENDING)
                .verdict(SubmissionVerdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier())
                + langConfig.getTimeLimitAllowance();
        int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier())
                + langConfig.getMemoryLimitAllowance();

        JudgeSdi sdi = JudgeSdi.builder()
                .submissionId(submission.getId())
                .problemId(problem.getId())
                .ruleType(contest != null ? contest.getRuleType().name() : problem.getRuleType().name())
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

        Submission finalSubmission = submission;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.JUDGE_ROUTING_KEY, sdi);
                log.info("Sent Submission [{}] to RabbitMQ AFTER DB COMMIT!", finalSubmission.getId());
            }
        });

        return submission.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public SubmissionDetailsSdo getSubmissionBasicInfo(UUID submissionId) {
        if (!submissionRepository.existsById(submissionId)) {
            throw new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND);
        }

        return submissionRepository.getDetails(submissionId);
    }

    @Override
    public RunCodeResponse getRunCodeResult(UUID token) {
        String redisKey = "RUN_CODE_RESULT:" + token.toString();

        String jsonResult = redisTemplate.opsForValue().get(redisKey);

        if (jsonResult != null) {
            try {
                return objectMapper.readValue(jsonResult, RunCodeResponse.class);
            } catch (Exception e) {
                log.error("Error processing Run Code result from Redis", e);
                throw new BusinessException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Failed to parse Run Code result.");
            }
        } else {
            log.info("Result is processing or removed from REDIS. Token: [{}]", token);
            throw new BusinessException(ErrorCode.RUN_CODE_IN_PROGRESS);
        }
    }

    @Override
    public Page<?> getSubmissions(UUID problemId,
            UUID userId,
            SubmissionVerdict submissionVerdict,
            String keyword,
            EStatus status,
            ProblemStatus problemStatus,
            EStatus submissionStatus,
            String languageKey,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            List<SubmissionVerdict> allowedVerdicts,
            boolean hideStaff,
            boolean ignoreContestPrivacy,
            boolean isPracticeOnly,
            Pageable pageable) {

        if (problemId != null && !problemRepository.existsById(problemId)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String searchKeyword = EscapeHelper.escapeLike(keyword);

        return submissionRepository.getSubmissions(problemId, userId, submissionVerdict, searchKeyword, status,
                problemStatus, submissionStatus, languageKey, fromDate, toDate, allowedVerdicts, hideStaff,
                ignoreContestPrivacy, isPracticeOnly, pageable);
    }

    @Override
    public ProblemStatisticSdo getProblemStatistics(UUID problemId, List<SubmissionVerdict> allowedVerdicts) {
        List<SubmissionRepository.VerdictCountProjection> projections = submissionRepository
                .countSubmissionsByVerdict(problemId);

        long total = 0;
        Map<String, Long> counts = new HashMap<>();

        for (SubmissionVerdict v : allowedVerdicts) {
            counts.put(v.name(), 0L);
        }

        for (SubmissionRepository.VerdictCountProjection p : projections) {
            SubmissionVerdict verdict = SubmissionVerdict.valueOf(p.getVerdict());

            if (!allowedVerdicts.contains(verdict)) {
                continue;
            }

            long count = p.getCount();
            counts.put(verdict.name(), count);
            total += count;
        }

        return ProblemStatisticSdo.builder()
                .totalSubmissions(total)
                .verdictCounts(counts)
                .build();
    }

    @Override
    public String getLatestSubmissionCode(UUID problemId, UUID userId, String languageKey) {
        return submissionRepository
                .findFirstSourceCodeByProblemIdAndUserIdAndLanguageKey(problemId, userId, languageKey).orElse(null);
    }

    @Override
    @Transactional
    public void rejudgeSubmissions(RejudgeSdi request) {
        log.info("Starting Rejudge request: {}", request);
        List<UUID> targetIds = new ArrayList<>();

        if (request.getSubmissionIds() != null && !request.getSubmissionIds().isEmpty()) {
            targetIds.addAll(request.getSubmissionIds());
        } else if (request.getProblemId() != null) {
            targetIds.addAll(submissionRepository.findIdsByProblemId(request.getProblemId()));
            log.info("Found {} submissions for Problem [{}] to rejudge", targetIds.size(), request.getProblemId());
        } else if (request.getContestId() != null) {
            targetIds.addAll(submissionRepository.findIdsByContestId(request.getContestId()));
            log.info("Found {} submissions for Contest [{}] to rejudge", targetIds.size(), request.getContestId());
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cần truyền submissionIds, problemId, hoặc contestId.");
        }

        if (targetIds.isEmpty()) {
            log.info("No submissions found to rejudge.");
            return;
        }

        // Chia mảng thành các batch 1000 ID để update status
        int batchSize = 1000;
        for (int i = 0; i < targetIds.size(); i += batchSize) {
            List<UUID> batchIds = targetIds.subList(i, Math.min(i + batchSize, targetIds.size()));
            submissionRepository.markSubmissionsForRejudge(batchIds);
        }

        // Bắn vào Message Queue (cũng chia batch để tránh OutOfMemory)
        for (int i = 0; i < targetIds.size(); i += batchSize) {
            List<UUID> batchIds = targetIds.subList(i, Math.min(i + batchSize, targetIds.size()));
            List<Submission> submissionsBatch = submissionRepository.findSubmissionsWithRulesByIds(batchIds);

            for (Submission submission : submissionsBatch) {
                try {
                    Problem problem = submission.getProblem();
                    LanguageConfig langConfig = languageLoader.getConfigByKey(submission.getLanguageKey());

                    if (langConfig == null) {
                        log.warn("Skipping rejudge for submission {} because language {} is no longer supported",
                                submission.getId(), submission.getLanguageKey());
                        continue;
                    }

                    int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier())
                            + langConfig.getTimeLimitAllowance();
                    int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier())
                            + langConfig.getMemoryLimitAllowance();

                    JudgeSdi sdi = JudgeSdi.builder()
                            .submissionId(submission.getId())
                            .problemId(problem.getId())
                            .ruleType(submission.getContest() != null ? submission.getContest().getRuleType().name()
                                    : problem.getRuleType().name())
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

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE,
                                    RabbitMQConfig.JUDGE_ROUTING_KEY, sdi);
                        }
                    });
                } catch (Exception e) {
                    log.error("Error building JudgeSdi for rejudging submission {}: {}", submission.getId(),
                            e.getMessage());
                }
            }
        }
        log.info("Successfully queued {} submissions for rejudging.", targetIds.size());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteSubmissions(List<UUID> ids) {
        if (ids == null || ids.isEmpty())
            return;
        submissionRepository.updateStatusForIds(ids, EStatus.DELETED);
        triggerBulkRecalculations(ids);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void voidSubmissions(List<UUID> ids) {
        if (ids == null || ids.isEmpty())
            return;
        submissionRepository.updateStatusForIds(ids, EStatus.INACTIVE);
        triggerBulkRecalculations(ids);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void restoreSubmissions(List<UUID> ids) {
        if (ids == null || ids.isEmpty())
            return;
        submissionRepository.updateStatusForIds(ids, EStatus.ACTIVE);
        triggerBulkRecalculations(ids);
    }

    private void triggerBulkRecalculations(List<UUID> submissionIds) {
        log.info("Registering Bulk Native Recalculations in background after commit for {} submissions",
                submissionIds.size());

        // 1. Find impacted relations within the current transaction
        List<Object[]> impacts = submissionRepository.findImpactedRelations(submissionIds);

        // 2. Schedule Async task to run AFTER the transaction is committed
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                log.info("[Background Job] Starting Native Recalculations for {} submissions...",
                                        submissionIds.size());

                                transactionTemplate.execute(status -> {
                                    Set<UUID> uniqueUserIds = new HashSet<>();
                                    Set<UUID> uniqueProblemIds = new HashSet<>();
                                    Set<UUID> uniqueContestIds = new HashSet<>();
                                    Set<String> processedPairs = new HashSet<>();

                                    for (Object[] row : impacts) {
                                        UUID userId = (UUID) row[0];
                                        UUID problemId = (UUID) row[1];
                                        UUID contestId = (UUID) row[2];

                                        if (userId != null)
                                            uniqueUserIds.add(userId);
                                        if (problemId != null)
                                            uniqueProblemIds.add(problemId);
                                        if (contestId != null)
                                            uniqueContestIds.add(contestId);

                                        if (userId != null && problemId != null) {
                                            String pairKey = userId + "_" + problemId;
                                            if (!processedPairs.contains(pairKey)) {
                                                userProblemStatusRepository.recalculateStatus(userId, problemId);
                                                processedPairs.add(pairKey);
                                            }
                                        }
                                    }

                                    for (UUID contestId : uniqueContestIds)
                                        contestParticipationRepository.recalculateOiScoresByContestId(contestId);
                                    for (UUID problemId : uniqueProblemIds)
                                        problemRepository.recalculateProblemStats(problemId);
                                    for (UUID userId : uniqueUserIds)
                                        userRepository.recalculateUserStats(userId);

                                    return null;
                                });

                                log.info("[Background Job] Completed Bulk Recalculations successfully.");
                            } catch (Exception e) {
                                log.error("[Background Job] Error during Bulk Recalculations: {}", e.getMessage(), e);
                            }
                        });
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionStatusSdo> checkSubmissionStatuses(List<UUID> ids) {
        if (ids == null || ids.isEmpty())
            return Collections.emptyList();
        return submissionRepository.findSubmissionStatusesByIds(ids);
    }
}