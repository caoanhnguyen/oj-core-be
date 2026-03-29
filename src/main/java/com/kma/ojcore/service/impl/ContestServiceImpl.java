package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.entity.*;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.EStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    public Page<ContestBasicSdo> searchAdminContests(String keyword,
                                                     RuleType ruleType,
                                                     ContestStatus contestStatus,
                                                     ContestVisibility visibility,
                                                     EStatus status,
                                                     Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = (contestStatus != null) ? contestStatus.name() : null;
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatusStr, visibility, status, pageable)
                .map(sdo -> {
                    sdo.setContestStatus(contestMapper.getRealTimeStatus(sdo.getStartTime(), sdo.getEndTime()));
                    return sdo;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestAdminSdo getAdminContestById(UUID contestId) {
        Contest contest = contestRepository.findContestWithAuthorByIdAndStatus(contestId, null)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestAdminSdo sdo = contestMapper.toAdminSdo(contest);
        sdo.setParticipantCount(contestParticipationRepository.countByContestId(contestId));
        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo createContest(CreateContestSdi req, UUID authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Contest contest = contestMapper.toEntity(req);
        contest.setAuthor(author);
        contest.setStatus(EStatus.INACTIVE);

        Contest saved = contestRepository.save(contest);
        log.info("Created new contest: {}", saved.getId());

        ContestAdminSdo sdo = contestMapper.toAdminSdo(saved);
        sdo.setParticipantCount(0L);
        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req) {
        Contest contest = contestRepository.findContestWithAuthorByIdAndStatus(contestId, null)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        if (req.getVisibility() == ContestVisibility.PUBLIC) {
            req.setPassword(null);
        }

        contestMapper.updateEntityFromSdi(req, contest);
        Contest updated = contestRepository.save(contest);
        log.info("Updated contest: {}", updated.getId());

        ContestAdminSdo sdo = contestMapper.toAdminSdo(updated);
        sdo.setParticipantCount(contestParticipationRepository.countByContestId(contestId));
        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void restoreContest(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() != EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only contests in DELETED status can be restored.");
        }

        contest.setStatus(EStatus.INACTIVE);
        log.info("Contest {} restored to INACTIVE", contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteContest(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED) return;

        contest.setStatus(EStatus.DELETED);
        log.info("Contest {} soft deleted", contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void togglePublishStatus(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contests in DELETED can not be published/unpublished.");
        }

        boolean isCurrentlyActive = contest.getStatus() == EStatus.ACTIVE;

        EStatus targetStatus = isCurrentlyActive ? EStatus.INACTIVE : EStatus.ACTIVE;

        contest.setStatus(targetStatus);
        log.info("Contest {} status changed to {}", contestId, targetStatus);
    }

    // ==================================================================== //
    // Problem Contest

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void addProblemsToContest(UUID contestId, List<AddContestProblemSdi> requests) {
        if (requests == null || requests.isEmpty()) return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest. Please restore it first.");
        }

        if (contest.getStatus() == EStatus.ACTIVE) {
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus != ContestStatus.UPCOMING) {
                // Không cho thêm đề khi đang thi hoặc đã kết thúc
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot add problems to a contest that is already ongoing or has ended.");
            }
        }

        Set<UUID> reqProblemIds = requests.stream().map(AddContestProblemSdi::getProblemId).collect(Collectors.toSet());
        Set<String> reqDisplayIds = requests.stream().map(AddContestProblemSdi::getDisplayId).collect(Collectors.toSet());

        // Check trùng lặp ngay trong payload
        if (reqProblemIds.size() < requests.size() || reqDisplayIds.size() < requests.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duplicate Problem ID or Display ID found in the request payload.");
        }

        long validProblemCount = problemRepository.countByIdInAndStatusNot(reqProblemIds, EStatus.DELETED);
        if (validProblemCount != reqProblemIds.size()) {
            // Có bài tập không tồn tại hoặc bị xóa
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND, "One or more problems do not exist or have been deleted.");
        }

        List<ContestProblemSdo> existing = contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
        Set<UUID> existingProblemIds = existing.stream().map(ContestProblemSdo::getProblemId).collect(Collectors.toSet());
        Set<String> existingDisplayIds = existing.stream().map(ContestProblemSdo::getDisplayId).collect(Collectors.toSet());

        List<ContestProblem> toSave = new ArrayList<>();

        for (AddContestProblemSdi req : requests) {
            if (existingProblemIds.contains(req.getProblemId()) || existingDisplayIds.contains(req.getDisplayId())) {
                // Nếu đã có Problem ID hoặc Display ID trùng với đề đã tồn tại trong contest thì báo lỗi, không cho thêm
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Conflict detected for Problem ID or Display ID: " + req.getDisplayId());
            }

            toSave.add(ContestProblem.builder()
                    .contest(contest)
                    .problem(problemRepository.getReferenceById(req.getProblemId())) // Lấy reference để tránh query thừa
                    .displayId(req.getDisplayId())
                    .points(req.getPoints())
                    .sortOrder(req.getSortOrder())
                    .build());
        }

        contestProblemRepository.saveAll(toSave);
        log.info("Added {} problems to contest {}", toSave.size(), contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void removeProblemsFromContest(UUID contestId, List<UUID> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest. Please restore it first.");
        }

        if (contest.getStatus() == EStatus.ACTIVE) {
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus != ContestStatus.UPCOMING) {
                // Thông báo cấm rút đề khi đang thi
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot remove problems from a contest that is already ongoing or has ended.");
            }
        }

        contestProblemRepository.deleteByContestIdAndProblemIdIn(contestId, problemIds);
        log.info("Removed {} problems from contest {}", problemIds.size(), contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        return contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
    }

    // ==================================================================== //
    // Participants

    @Transactional(readOnly = true)
    @Override
    public Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword, Boolean isDisqualified, Pageable pageable) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found.");
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);
        return contestParticipationRepository.searchParticipants(contestId, escapedKeyword, isDisqualified, pageable);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void disqualifyUsers(UUID contestId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest.");
        }

        int bannedCount = contestParticipationRepository.banUsersInBulk(contestId, userIds);
        log.info("BULK BANNED: {} users from Contest {}", bannedCount, contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void requalifyUsers(UUID contestId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest.");
        }

        int unbannedCount = contestParticipationRepository.unbanUsersInBulk(contestId, userIds);
        log.info("BULK UNBANNED: {} users in Contest {}", unbannedCount, contestId);
    }

    // ==================================================================== //
    // USER

    @Transactional(readOnly = true)
    @Override
    public Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = (contestStatus != null) ? contestStatus.name() : null;
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatusStr, null, EStatus.ACTIVE, pageable).map(contest -> {
            contest.setContestStatus(contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()));
            return contest;
        });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findContestWithAuthorByIdAndStatus(contestId, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestDetailSdo sdo = contestMapper.toDetailSdo(contest);
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
        Contest contest = contestRepository.findByIdAndStatusActive(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contestParticipationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
        }

        if (contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()) == ContestStatus.ENDED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contest has already ended.");
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
        Contest contest = contestRepository.findByIdAndStatusActive(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        // 2. Chặn thi trước giờ
        if (contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()) == ContestStatus.UPCOMING) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_STARTED);
        }

        // 3. Lôi Participation lên để check cả 2 case: Đã đăng ký chưa? Có bị Ban không?
        ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_REGISTERED));

        // Nếu bị Admin trảm thì sút luôn, cấm xem đề!
        if (participation.isDisqualified()) {
            throw new BusinessException(ErrorCode.BANNED_FROM_CONTEST, "You are banned from participating in this contest.");
        }

        return contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContestParticipantPublicSdo> getPublicContestParticipants(UUID contestId, String keyword, Pageable pageable) {
        if(!contestRepository.existsByIdAndStatus(contestId, EStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);

        return contestParticipationRepository.searchPublicParticipants(contestId, escapedKeyword, pageable);
    }
}