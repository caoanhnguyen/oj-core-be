package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.ContestAdminSdo;
import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.dto.response.contests.ContestDetailSdo;
import com.kma.ojcore.dto.response.contests.ContestProblemSdo;
import com.kma.ojcore.entity.*;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.ContestMapper;
import com.kma.ojcore.repository.*;
import com.kma.ojcore.service.ContestService;
import com.kma.ojcore.utils.EscapeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestServiceImpl implements ContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final ContestMapper contestMapper;
    private final ContestParticipationRepository contestParticipationRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<ContestAdminSdo> searchAdminContests(String keyword,
                                                     RuleType ruleType,
                                                     ContestStatus contestStatus,
                                                     Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatus, pageable)
                .map(contestMapper::toAdminSdo);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo createContest(CreateContestSdi req, UUID authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Start time must be before end time.");
        }

        Contest contest = contestMapper.toEntity(req);
        contest.setAuthor(author);
        contest.setContestStatus(ContestStatus.UPCOMING);

        Contest saved = contestRepository.save(contest);
        log.info("Created new contest: {}", saved.getId());
        return contestMapper.toAdminSdo(saved);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Start time must be before end time.");
        }

        contestMapper.updateEntityFromSdi(req, contest);
        Contest updated = contestRepository.save(contest);
        log.info("Updated contest: {}", updated.getId());
        return contestMapper.toAdminSdo(updated);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteContest(UUID contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }
        contestRepository.deleteById(contestId);
        log.info("Deleted contest: {}", contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public ContestAdminSdo getAdminContestById(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));
        return contestMapper.toAdminSdo(contest);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void addProblemToContest(UUID contestId, AddContestProblemSdi req) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        Problem problem = problemRepository.findById(req.getProblemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        if (contestProblemRepository.existsByContestIdAndProblemId(contestId, req.getProblemId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Problem already exists in this contest.");
        }

        if (contestProblemRepository.existsByContestIdAndDisplayId(contestId, req.getDisplayId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Display ID already exists in this contest.");
        }

        ContestProblem cp = ContestProblem.builder()
                .contest(contest)
                .problem(problem)
                .displayId(req.getDisplayId())
                .points(req.getPoints())
                .sortOrder(req.getSortOrder())
                .build();

        contestProblemRepository.save(cp);
        log.info("Added problem {} to contest {}", problem.getId(), contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void removeProblemFromContest(UUID contestId, UUID problemId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestProblem cp = contest.getProblems().stream()
                .filter(p -> p.getProblem().getId().equals(problemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Problem not found in this contest."));

        contest.getProblems().remove(cp);
        contestRepository.save(contest);
        log.info("Removed problem {} from contest {}", problemId, contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        return contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId).stream()
                .map(cp -> ContestProblemSdo.builder()
                        .id(cp.getId())
                        .problemId(cp.getProblem().getId())
                        .problemSlug(cp.getProblem().getSlug())
                        .originalTitle(cp.getProblem().getTitle())
                        .displayId(cp.getDisplayId())
                        .points(cp.getPoints())
                        .sortOrder(cp.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    // USER

    private ContestStatus getRealTimeStatus(Contest contest) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);
        if (now.isBefore(contest.getStartTime())) {
            return ContestStatus.UPCOMING;
        } else if (now.isAfter(contest.getEndTime())) {
            return ContestStatus.ENDED;
        }
        return ContestStatus.ONGOING;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatus, pageable).map(contest -> {
            ContestBasicSdo sdo = contestMapper.toBasicSdo(contest);
            sdo.setContestStatus(getRealTimeStatus(contest));
            sdo.setParticipantCount(contestParticipationRepository.countByContestId(contest.getId()));
            return sdo;
        });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestDetailSdo sdo = contestMapper.toDetailSdo(contest);
        sdo.setContestStatus(getRealTimeStatus(contest));
        sdo.setParticipantCount(contestParticipationRepository.countByContestId(contest.getId()));

        if (userId != null) {
            sdo.setRegistered(contestParticipationRepository.existsByContestIdAndUserId(contestId, userId));
        } else {
            sdo.setRegistered(false);
        }

        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void registerContest(UUID contestId, UUID userId, RegisterContestSdi req) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contestParticipationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
        }

        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            if (req == null || req.getPassword() == null || !req.getPassword().equals(contest.getPassword())) {
                throw new BusinessException(ErrorCode.INCORRECT_PASSWORD);
            }
        }

        User user = userRepository.getReferenceById(userId);

        ContestParticipation participation = ContestParticipation.builder()
                .contest(contest)
                .user(user)
                .isRegistered(true)
                .build();

        contestParticipationRepository.save(participation);
        log.info("User {} registered for contest {}", userId, contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContestProblemSdo> getContestProblemsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (!contestParticipationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED);
        }

        if (getRealTimeStatus(contest) == ContestStatus.UPCOMING) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_STARTED);
        }

        return contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId).stream()
                .map(cp -> ContestProblemSdo.builder()
                        .id(cp.getId())
                        .problemId(cp.getProblem().getId())
                        .problemSlug(cp.getProblem().getSlug())
                        .originalTitle(cp.getProblem().getTitle())
                        .displayId(cp.getDisplayId())
                        .points(cp.getPoints())
                        .sortOrder(cp.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }
}