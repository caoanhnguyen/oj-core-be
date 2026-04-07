package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.entity.*;
import com.kma.ojcore.enums.*;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.ContestMapper;
import com.kma.ojcore.repository.*;
import com.kma.ojcore.service.ContestService;
import com.kma.ojcore.utils.EscapeHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final SubmissionRepository submissionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${REDIS_PREFIX_LEADERBOARD:CONTEST_LEADERBOARD:}")
    private String leaderboardPrefix;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardCacheWrapper {
        private List<ContestLeaderboardSdo> content;
        private long totalElements;
    }

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
        return contestRepository
                .searchAdminContests(searchKeyword, ruleType, contestStatusStr, visibility, status, pageable)
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

        if (req.getFormat() == ContestFormat.WINDOWED) {
            if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Windowed contests must have a valid duration.");
            }
        } else {
            req.setDurationMinutes(null);
        }

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

        if (req.getFormat() == ContestFormat.WINDOWED) {
            if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Windowed contests must have a valid duration.");
            }
        } else {
            req.setDurationMinutes(null);
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
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Only contests in DELETED status can be restored.");
        }

        contest.setStatus(EStatus.INACTIVE);
        log.info("Contest {} restored to INACTIVE", contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void softDeleteContest(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED)
            return;

        contest.setStatus(EStatus.DELETED);
        log.info("Contest {} soft deleted", contestId);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void togglePublishStatus(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Contests in DELETED can not be published/unpublished.");
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
        if (requests == null || requests.isEmpty())
            return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cannot modify a deleted contest. Please restore it first.");
        }

        if (contest.getStatus() == EStatus.ACTIVE) {
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus != ContestStatus.UPCOMING) {
                // Không cho thêm đề khi đang thi hoặc đã kết thúc
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Cannot add problems to a contest that is already ongoing or has ended.");
            }
        }

        Set<UUID> reqProblemIds = requests.stream().map(AddContestProblemSdi::getProblemId).collect(Collectors.toSet());
        Set<String> reqDisplayIds = requests.stream().map(AddContestProblemSdi::getDisplayId)
                .collect(Collectors.toSet());

        // Check trùng lặp ngay trong payload
        if (reqProblemIds.size() < requests.size() || reqDisplayIds.size() < requests.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Duplicate Problem ID or Display ID found in the request payload.");
        }

        long validProblemCount = problemRepository.countByIdInAndStatusNot(reqProblemIds, EStatus.DELETED);
        if (validProblemCount != reqProblemIds.size()) {
            // Có bài tập không tồn tại hoặc bị xóa
            throw new BusinessException(ErrorCode.PROBLEM_NOT_FOUND,
                    "One or more problems do not exist or have been deleted.");
        }

        List<ContestProblemSdo> existing = contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);
        Set<UUID> existingProblemIds = existing.stream().map(ContestProblemSdo::getProblemId)
                .collect(Collectors.toSet());
        Set<String> existingDisplayIds = existing.stream().map(ContestProblemSdo::getDisplayId)
                .collect(Collectors.toSet());

        List<ContestProblem> toSave = new ArrayList<>();

        for (AddContestProblemSdi req : requests) {
            if (existingProblemIds.contains(req.getProblemId()) || existingDisplayIds.contains(req.getDisplayId())) {
                // Nếu đã có Problem ID hoặc Display ID trùng với đề đã tồn tại trong contest
                // thì báo lỗi, không cho thêm
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Conflict detected for Problem ID or Display ID: " + req.getDisplayId());
            }

            toSave.add(ContestProblem.builder()
                    .contest(contest)
                    .problem(problemRepository.getReferenceById(req.getProblemId()))
                    .displayId(req.getDisplayId())
                    .points(req.getPoints())
                    .sortOrder(req.getSortOrder())
                    .build());
        }

        contestProblemRepository.saveAll(toSave);
        log.info("Added {} problems to contest {}", toSave.size(), contestId);
        recalculateLeaderboardScores(contestId, contest);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void removeProblemsFromContest(UUID contestId, List<UUID> problemIds) {
        if (problemIds == null || problemIds.isEmpty())
            return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cannot modify a deleted contest. Please restore it first.");
        }

        if (contest.getStatus() == EStatus.ACTIVE) {
            ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
            if (timeStatus != ContestStatus.UPCOMING) {
                // Thông báo cấm rút đề khi đang thi
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Cannot remove problems from a contest that is already ongoing or has ended.");
            }
        }

        contestProblemRepository.deleteByContestIdAndProblemIdIn(contestId, problemIds);
        log.info("Removed {} problems from contest {}", problemIds.size(), contestId);
        recalculateLeaderboardScores(contestId, contest);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void updateProblemsInContest(UUID contestId, List<UpdateContestProblemSdi> requests) {
        if (requests == null || requests.isEmpty()) return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest.");
        }

        List<ContestProblem> existingProblems = contestProblemRepository.findByContestId(contestId);
        Map<UUID, ContestProblem> problemMap = existingProblems.stream()
                .collect(Collectors.toMap(cp -> cp.getProblem().getId(), cp -> cp)); // TODO: đang bị N+1 ở đây, cần optimize

        boolean updated = false;
        for (UpdateContestProblemSdi req : requests) {
            ContestProblem cp = problemMap.get(req.getProblemId());
            if (cp != null) {
                // Ignore matching checks for displayId overlaps within the same update list to avoid false positives. Just update directly.
                cp.setDisplayId(req.getDisplayId());
                cp.setPoints(req.getPoints());
                cp.setSortOrder(req.getSortOrder());
                updated = true;
            }
        }

        if (updated) {
            contestProblemRepository.saveAll(existingProblems);
            log.info("Updated problems config for contest {}", contestId);
            recalculateLeaderboardScores(contestId, contest);
        }
    }

    private void recalculateLeaderboardScores(UUID contestId, Contest contest) {
        if (contest.getRuleType() != RuleType.OI) {
            return; // Only OI requires automatic score recalculation upon problem updates
        }

        // Asynchronous Background Job via Spring's Thread Pool
        CompletableFuture.runAsync(() -> {
            try {
                // Optimized N+1: Delegate aggregation logic to the database engine directly.
                int updatedRows = contestParticipationRepository.recalculateOiScoresByContestId(contestId);

                // Evict obsolete Redis Leaderboard Caches
                Set<String> keys = redisTemplate.keys(leaderboardPrefix + contestId + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("[Background Job] Recalculation complete. Updated scores for {} participants in contest {}", updatedRows, contestId);
                
            } catch (Exception e) {
                log.error("[Background Job] Failed to recalculate leaderboard scores for contest {}: {}", contestId, e.getMessage());
            }
        });
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
    public Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword,
            Boolean isDisqualified, Pageable pageable) {
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found.");
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);
        return contestParticipationRepository.searchParticipants(contestId, escapedKeyword, isDisqualified, pageable);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void disqualifyUsers(UUID contestId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty())
            return;

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
        if (userIds == null || userIds.isEmpty())
            return;

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot modify a deleted contest.");
        }

        int unbannedCount = contestParticipationRepository.unbanUsersInBulk(contestId, userIds);
        log.info("BULK UNBANNED: {} users in Contest {}", unbannedCount, contestId);
    }

    // Leaderboard & Submissions

    @Transactional(readOnly = true)
    @Override
    public Page<ContestLeaderboardSdo> getContestLeaderboard(UUID contestId, EStatus status, Pageable pageable, boolean bypassVisibility) {
        // 1. Check chốt chặn Contest (Lấy từ DB lên cực nhanh vì dùng PK Indexed)
        Contest contest = contestRepository.findByIdAndStatus(contestId, status)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
        if (timeStatus == ContestStatus.UPCOMING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "The contest has not started yet. Leaderboard is hidden.");
        }

        // Kiểm tra quyền xem Scoreboard
        if (!bypassVisibility) {
            ScoreboardVisibility visibility = contest.getScoreboardVisibility();
            if (visibility == ScoreboardVisibility.HIDDEN_PERMANENTLY) {
                throw new BusinessException(ErrorCode.SCOREBOARD_HIDDEN);
            } else if (visibility == ScoreboardVisibility.HIDDEN_DURING_CONTEST) {
                if (LocalDateTime.now().isBefore(contest.getEndTime())) {
                    throw new BusinessException(ErrorCode.SCOREBOARD_HIDDEN);
                }
            }
        }

        // ==========================================
        // 2. KÍCH HOẠT LÁ CHẮN REDIS (TTL = 5 GIÂY)
        // ==========================================
        String redisKey = String.format("%s:%s:PAGE:%d:SIZE:%d", leaderboardPrefix,
                contestId, pageable.getPageNumber(), pageable.getPageSize());

        try {
            String cachedData = redisTemplate.opsForValue().get(redisKey);
            if (org.springframework.util.StringUtils.hasText(cachedData)) {
                // Nếu có trong Redis -> Bắn thẳng về cho User (DB không hề biết gì)
                LeaderboardCacheWrapper wrapper = objectMapper.readValue(cachedData, LeaderboardCacheWrapper.class);
                return new org.springframework.data.domain.PageImpl<>(
                        wrapper.getContent(), pageable, wrapper.getTotalElements());
            }
        } catch (Exception e) {
            log.error("Redis Cache Error for Leaderboard: {}", e.getMessage());
            // Lỗi Redis thì bỏ qua, chạy tiếp xuống DB chứ không làm chết API
        }

        // ==========================================
        // 3. NẾU REDIS TRỐNG -> CHỌC XUỐNG DB
        // ==========================================
        Page<ContestLeaderboardSdo> page = contestParticipationRepository.getLeaderboard(contestId, pageable);

        // 4. Lấy data từ DB xong -> Lưu lên Redis để phục vụ cho 5 giây tiếp theo
        try {
            LeaderboardCacheWrapper wrapper = new LeaderboardCacheWrapper(page.getContent(), page.getTotalElements());
            String jsonToCache = objectMapper.writeValueAsString(wrapper);

            // TTL = 5 giây là quá đủ để chặn hàng vạn request F5 cùng lúc
            redisTemplate.opsForValue().set(redisKey, jsonToCache, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to save Leaderboard to Redis: {}", e.getMessage());
        }

        return page;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SubmissionBasicSdo> getMyContestSubmissions(UUID contestId, UUID userId, UUID problemId, Pageable pageable) {
        if(!contestRepository.existsByIdAndStatus(contestId, EStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found.");
        }

        // Chặn xem nếu chưa đăng ký
        if (!contestParticipationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED, "You are not registered for this contest.");
        }

        return submissionRepository.findMyContestSubmissions(contestId, userId, problemId, pageable);
    }

    // ==================================================================== //
    // USER


    @Transactional(readOnly = true)
    @Override
    public List<MyActiveContestSdo> getMyActiveContests(UUID userId) {
        List<MyActiveContestSdo> sdos = contestParticipationRepository.findMyActiveContestSdos(userId);
        for (MyActiveContestSdo sdo : sdos) {
            ContestBasicSdo basicContest = sdo.getContest();
            basicContest.setContestStatus(contestMapper.getRealTimeStatus(basicContest.getStartTime(), basicContest.getEndTime()));
        }

        return sdos;
    }


    @Transactional(readOnly = true)
    @Override
    public Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus,
            Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = (contestStatus != null) ? contestStatus.name() : null;
        return contestRepository
                .searchAdminContests(searchKeyword, ruleType, contestStatusStr, null, EStatus.ACTIVE, pageable)
                .map(contest -> {
                    contest.setContestStatus(
                            contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()));
                    return contest;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findContestWithAuthorByIdAndStatus(contestId, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        ContestDetailSdo sdo = contestMapper.toDetailSdo(contest);
        sdo.setParticipantCount(contestParticipationRepository.countByContestId(contest.getId()));

        // 1. Bơm serverTime vào cho Frontend đồng bộ đồng hồ chống cheat
        sdo.setServerTime(java.time.LocalDateTime.now());

        // 2. Xử lý lấy thông tin User (Gộp làm 1 câu query duy nhất)
        if (userId != null) {
            ContestParticipation participation = contestParticipationRepository
                    .findByContestIdAndUserId(contestId, userId).orElse(null);

            if (participation != null) {
                sdo.setRegistered(true);
                sdo.setContestParticipation(contestMapper.toParticipationSdo(participation));
            } else {
                sdo.setRegistered(false);
                sdo.setContestParticipation(null);
            }
        } else {
            sdo.setRegistered(false);
            sdo.setContestParticipation(null);
        }

        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void registerContest(UUID contestId, UUID userId, RegisterContestSdi req) {
        Contest contest = contestRepository.findByIdAndStatus(contestId, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
        if (timeStatus == ContestStatus.ENDED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contest has already ended.");
        }

        if (contest.getFormat() == ContestFormat.STRICT) {
            if (timeStatus == ContestStatus.ONGOING && Boolean.FALSE.equals(contest.getAllowLateRegistration())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Late registration is not allowed for this contest.");
            }
        }

        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            if (req == null || req.getPassword() == null || !req.getPassword().equals(contest.getPassword())) { // TODO: Nên hash password thay vì lưu thẳng, nhưng tạm thời để vậy cho nhanh
                throw new BusinessException(ErrorCode.INCORRECT_PASSWORD);
            }
        }

        if (contestParticipationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
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

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public List<ContestProblemSdo> getContestProblemsForUser(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findByIdAndStatus(contestId, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestStatus timeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());

        // 1. Chặn thi trước giờ
        if (timeStatus == ContestStatus.UPCOMING) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_STARTED, "The contest has not started yet.");
        }

        // ========================================================
        // 2. CHỈ KIỂM TRA QUYỀN KHẮT KHE KHI KỲ THI ĐANG DIỄN RA (ONGOING)
        // ========================================================
        if (timeStatus == ContestStatus.ONGOING) {
            ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contestId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_REGISTERED, "You must register to view problems during an active contest."));

            if (participation.getIsDisqualified()) {
                throw new BusinessException(ErrorCode.BANNED_FROM_CONTEST, "You are banned from participating in this contest.");
            }

            if (participation.getStartTime() == null) {
                // Nếu là Windowed Contest -> Bắt buộc phải nhấn Start mới cho xem đề (để tính giờ cá nhân)
                if (contest.getFormat() == ContestFormat.WINDOWED) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You must start the contest to view problems.");
                }
                // Nếu là Fixed Contest -> Không cần nhấn Start, cứ đến giờ là cho xem
            }


            // Tự động tước quyền nếu Hết giờ cá nhân (Chỉ check nếu đã có endTime cá nhân)
            if (!participation.getIsFinished()
                    && participation.getEndTime() != null
                    && java.time.LocalDateTime.now().isAfter(participation.getEndTime())) {

                participation.setIsFinished(true);
                contestParticipationRepository.save(participation);
            }
        }

        // ========================================================
        // 3. NẾU timeStatus == ENDED -> BỎ QUA KIỂM TRA ĐĂNG KÝ (UPSOLVING)
        // Đi thẳng xuống logic lấy đề bài bên dưới
        // ========================================================

        List<ContestProblemSdo> contestProblems = contestProblemRepository.findByContestIdOrderBySortOrderAsc(contestId);

        // Đoạn lấy Verdict này vẫn chạy an toàn ngay cả khi User chưa đăng ký
        // (Vì findVerdictsByContestAndUser sẽ trả về list rỗng, không gây lỗi)
        List<SubmissionRepository.ProblemVerdictProjection> userSubmissions =
                submissionRepository.findVerdictsByContestAndUser(contestId, userId);

        java.util.Map<UUID, SubmissionVerdict> bestVerdicts = new java.util.HashMap<>();
        for (var sub : userSubmissions) {
            UUID pId = sub.getProblemId();
            SubmissionVerdict v = sub.getVerdict();

            if (v == SubmissionVerdict.AC) {
                bestVerdicts.put(pId, v);
            } else {
                bestVerdicts.putIfAbsent(pId, v);
            }
        }

        for (ContestProblemSdo sdo : contestProblems) {
            sdo.setSubmissionVerdict(bestVerdicts.get(sdo.getProblemId()));
        }

        return contestProblems;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContestParticipantPublicSdo> getPublicContestParticipants(UUID contestId, String keyword,
            Pageable pageable) {
        if (!contestRepository.existsByIdAndStatus(contestId, EStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);

        return contestParticipationRepository.searchPublicParticipants(contestId, escapedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SubmissionBasicSdo> getAdminContestSubmissions(UUID contestId, Pageable pageable) {
        // Admin thì cứ tồn tại Contest là soi được hết
        if (!contestRepository.existsById(contestId)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found.");
        }

        return submissionRepository.findAllContestSubmissions(contestId, pageable);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestParticipationSdo startContest(UUID contestId, UUID userId) {
        Contest contest = contestRepository.findByIdAndStatus(contestId, EStatus.ACTIVE)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found or not active."));

        // Sửa đoạn này:
        ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contestId, userId)
                .orElseGet(() -> {
                    // Nếu chưa tìm thấy Participation (chưa đăng ký)
                    if (contest.getFormat() == ContestFormat.WINDOWED && contest.getVisibility() == ContestVisibility.PUBLIC) {
                        // Tự động tạo bản ghi đăng ký ngầm luôn CHỈ dành cho WINDOWED PUBLIC
                        return ContestParticipation.builder()
                                .contest(contest)
                                .user(userRepository.getReferenceById(userId))
                                .isRegistered(true)
                                .build();
                    } else if (contest.getFormat() == ContestFormat.STRICT) {
                         // STRICT Contest (Dù Public hay Private) ĐỀU bắt buộc nhấn nút Register riêng biệt
                        throw new BusinessException(ErrorCode.NOT_REGISTERED, "You must explicitly register for this Strict Contest.");
                    } else {
                        // WINDOWED mà Private thì cũng phải gọi Register trước để nhập pass
                        throw new BusinessException(ErrorCode.NOT_REGISTERED, "You must register (with password) to join this Private Windowed contest.");
                    }
                });


        if (participation.getIsDisqualified()) {
            throw new BusinessException(ErrorCode.BANNED_FROM_CONTEST, "You are disqualified from this contest.");
        }

        if (participation.getStartTime() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You have already started this contest.");
        }

        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra Cửa sổ thời gian chung (Global Time Window)
        if (now.isBefore(contest.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "The contest has not opened yet.");
        }
        if (now.isAfter(contest.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "The contest has already ended.");
        }

        // Bắt đầu tính giờ cho cá nhân
        participation.setStartTime(now);

        // Thuật toán ghim giờ (Dựa trên Format Kỳ Thi)
        if (contest.getFormat() == ContestFormat.WINDOWED) {
            LocalDateTime predictedEndTime = now.plusMinutes(contest.getDurationMinutes());
            // Lấy mốc thời gian nào đến TRƯỚC: Hết giờ làm bài cá nhân hay Đóng cửa kỳ thi
            participation.setEndTime(
                    predictedEndTime.isBefore(contest.getEndTime()) ? predictedEndTime : contest.getEndTime());
        } else {
            // Nếu Format là STRICT -> Fixed Contest (Tất cả nộp bài cùng lúc)
            participation.setEndTime(contest.getEndTime());
        }

        participation = contestParticipationRepository.save(participation);
        log.info("User {} started contest {}. Session ends at {}", userId, contestId, participation.getEndTime());

        return contestMapper.toParticipationSdo(participation);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void finishContest(UUID contestId, UUID userId) {
        ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_REGISTERED,
                        "You are not registered for this contest."));

        if (participation.getStartTime() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You haven't started the contest yet.");
        }

        if (participation.getIsFinished()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Your contest session has already finished.");
        }

        // Đánh dấu nộp bài sớm/Thoát phòng thi
        participation.setIsFinished(true);
        contestParticipationRepository.save(participation);

        log.info("User {} finished contest {} early.", userId, contestId);
    }
}