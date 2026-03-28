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
                                                     Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = contestStatus != null ? contestStatus.name() : null;
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatusStr, null, null, pageable)
                .map(sdo -> {
                    sdo.setContestStatus(contestMapper.getRealTimeStatus(sdo.getStartTime(), sdo.getEndTime()));
                    return sdo;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestAdminSdo getAdminContestById(UUID contestId) {
        Contest contest = contestRepository.findContestWithAuthorById(contestId)
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

        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Start time must be before end time.");
        }

        Contest contest = contestMapper.toEntity(req);
        contest.setAuthor(author);

        Contest saved = contestRepository.save(contest);
        log.info("Created new contest: {}", saved.getId());

        ContestAdminSdo sdo = contestMapper.toAdminSdo(saved);
        sdo.setParticipantCount(0L);
        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req) {
        Contest contest = contestRepository.findContestWithAuthorById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Start time must be before end time.");
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
    public void addProblemsToContest(UUID contestId, List<AddContestProblemSdi> requests) {
        if (requests == null || requests.isEmpty()) return;

        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        // 1. Tách List ID và Display ID ra (Dùng Set để tự động loại bỏ trùng lặp)
        Set<UUID> reqProblemIds = requests.stream().map(AddContestProblemSdi::getProblemId).collect(Collectors.toSet());
        Set<String> reqDisplayIds = requests.stream().map(AddContestProblemSdi::getDisplayId).collect(Collectors.toSet());

        // Nếu kích thước Set nhỏ hơn kích thước Request -> Có thằng bị trùng lặp ngay trong lúc gửi lên!
        if (reqProblemIds.size() < requests.size() || reqDisplayIds.size() < requests.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Duplicate Problem ID or Display ID in the request payload.");
        }

        // 2. Chọc DB đúng 1 phát để check xem tất cả Problem này có thật sự tồn tại không
        long validProblemCount = problemRepository.countByIdIn(reqProblemIds);
        if (validProblemCount != reqProblemIds.size()) {
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        // 3. Lấy bài cũ ra đối chiếu (1 câu Query)
        List<ContestProblemSdo> existing = contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
        Set<UUID> existingProblemIds = existing.stream().map(ContestProblemSdo::getProblemId).collect(Collectors.toSet());
        Set<String> existingDisplayIds = existing.stream().map(ContestProblemSdo::getDisplayId).collect(Collectors.toSet());

        List<ContestProblem> toSave = new ArrayList<>();

        for (AddContestProblemSdi req : requests) {
            // Check xung đột với những bài ĐÃ CÓ trong Contest
            if (existingProblemIds.contains(req.getProblemId()) || existingDisplayIds.contains(req.getDisplayId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Conflict in Problem ID or Display ID: " + req.getDisplayId());
            }

            // Vòng for này giờ SẠCH BÓNG, không có 1 câu Query nào chọc xuống DB nữa!
            toSave.add(ContestProblem.builder()
                    .contest(contestRepository.getReferenceById(contestId))
                    .problem(problemRepository.getReferenceById(req.getProblemId()))
                    .displayId(req.getDisplayId())
                    .points(req.getPoints())
                    .sortOrder(req.getSortOrder())
                    .build());
        }

        // 4. Bắn 1 lô xuống DB!
        contestProblemRepository.saveAll(toSave);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void removeProblemsFromContest(UUID contestId, List<UUID> problemIds) {
        if (!contestRepository.existsById(contestId)) throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        contestProblemRepository.deleteByContestIdAndProblemIdIn(contestId, problemIds);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        return contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void disqualifyUsers(UUID contestId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        // Chỉ check tồn tại contest (1 query cực nhẹ)
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        // 1 nhát kiếm chém bay tất cả! Trả về số lượng người thực tế đã bị ban.
        int bannedCount = contestParticipationRepository.banUsersInBulk(contestId, userIds);

        log.info("BULK BANNED: {} users from Contest {}", bannedCount, contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void updateContestVisibility(UUID contestId, boolean isVisible) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.isVisible() == isVisible) {
            return;
        }

        contest.setVisible(isVisible);
        contestRepository.save(contest);

        log.info("Contest {} visibility changed to: {}", contestId, isVisible ? "PUBLISHED" : "DRAFT");
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void restoreContest(UUID contestId) {
        log.info("Restoring contest: {}", contestId);
        if(!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }
        contestRepository.updateStatusById(EStatus.ACTIVE, contestId);
        log.info("Contest {} restored", contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteContest(UUID contestId) {
        log.info("Soft deleting contest: {}", contestId);
        if(!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }
        contestRepository.updateStatusById(EStatus.DELETED, contestId);
        log.info("Contest {} soft deleted", contestId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword, Boolean isDisqualified, Pageable pageable) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);

        return contestParticipationRepository.searchParticipants(contestId, escapedKeyword, isDisqualified, pageable);
    }

    // ==================================================================== //
    // USER

    @Transactional(readOnly = true)
    @Override
    public Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = contestStatus != null ? contestStatus.name() : null;
        return contestRepository.searchAdminContests(searchKeyword, ruleType, contestStatusStr, true, EStatus.ACTIVE, pageable).map(contest -> {
            contest.setContestStatus(contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()));
            return contest;
        });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findContestWithAuthorById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (!contest.isVisible()) throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);

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
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (!contest.isVisible()) throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);

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
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        // 1. Chặn xem đề nếu Contest bị ẩn (Draft)
        if (!contest.isVisible()) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        // 2. Chặn thi trước giờ
        if (contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()) == ContestStatus.UPCOMING) {
            System.out.println(contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()));
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
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        // Contest chưa Public thì cấm xem danh sách!
        if (contest.getStatus() != EStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);

        return contestParticipationRepository.searchPublicParticipants(contestId, escapedKeyword, pageable);
    }
}