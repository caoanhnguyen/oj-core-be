package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.config.LanguageLoader;
import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.submissions.JudgeSdi;
import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.dto.response.submissions.SubmissionDetailsSdo;
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
    private final ContestRepository contestRepository;
    private final ContestParticipationRepository contestParticipationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestMapper contestMapper;

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

        Contest contest;
        if (request.getContestId() != null) {
            contest = contestRepository.findByIdAndStatusActive(request.getContestId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found or not active."));

            // Luật 1: Chặn đứng kỳ thi chưa bắt đầu. Cho phép nộp khi đang diễn ra (Thi) hoặc đã kết thúc (Upsolving).
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus == ContestStatus.UPCOMING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contest has not started yet. You cannot submit solutions at this time.");
            }

            // Luật 2: Kiểm tra User đã đăng ký chưa và có bị Ban không?
            ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contest.getId(), currentUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_REGISTERED, "You are not registered for this contest."));
            if (participation.isDisqualified()) {
                throw new BusinessException(ErrorCode.BANNED_FROM_CONTEST, "You are disqualified from this contest.");
            }

            // Luật 3: Bài toán này có nằm trong Contest không?
            if (!contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "This problem does not belong to the requested contest.");
            }
        }

        User user = userRepository.getReferenceById(currentUserId);

        Submission submission = Submission.builder()
                .problem(problem)
                .user(user)
                .languageKey(request.getLanguageKey())
                .sourceCode(request.getSourceCode())
                .submissionStatus(SubmissionStatus.PENDING)
                .verdict(SubmissionVerdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        int finalTimeLimit = (int) (problem.getTimeLimitMs() * langConfig.getTimeMultiplier()) + langConfig.getTimeLimitAllowance();
        int finalMemoryLimit = (int) (problem.getMemoryLimitMb() * langConfig.getMemoryMultiplier()) + langConfig.getMemoryLimitAllowance();

        JudgeSdi sdi = JudgeSdi.builder()
                .submissionId(submission.getId())
                .problemId(problem.getId())
                .ruleType(problem.getRuleType().name())
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
        if(!submissionRepository.existsById(submissionId)) {
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
                                  List<SubmissionVerdict> allowedVerdicts,
                                  boolean hideStaff,
                                  Pageable pageable) {

        if (problemId != null && !problemRepository.existsById(problemId)) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String searchKeyword = EscapeHelper.escapeLike(keyword);

        return submissionRepository.getSubmissions(problemId, userId, submissionVerdict, searchKeyword, status, problemStatus, allowedVerdicts, hideStaff, pageable);
    }

    @Override
    public ProblemStatisticSdo getProblemStatistics(UUID problemId, List<SubmissionVerdict> allowedVerdicts) {
        List<SubmissionRepository.VerdictCountProjection> projections =
                submissionRepository.countSubmissionsByVerdict(problemId);

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
        return submissionRepository.findFirstSourceCodeByProblemIdAndUserIdAndLanguageKey(problemId, userId, languageKey).orElse(null);
    }
}