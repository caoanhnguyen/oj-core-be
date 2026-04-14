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
import com.kma.ojcore.utils.UuidHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private final ContestParticipationProblemRepository cppRepository;
    private final com.kma.ojcore.repository.ContestWhitelistRepository contestWhitelistRepository;
    private final SubmissionRepository submissionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${REDIS_PREFIX_LEADERBOARD:CONTEST_LEADERBOARD:}")
    private String leaderboardPrefix;

    // No longer needed, using ContestLeaderboardPageSdo directly for caching


    @Transactional(readOnly = true)
    @Override
    public Page<ContestBasicSdo> searchAdminContests(String keyword,
            RuleType ruleType,
            ContestStatus contestStatus,
            ContestVisibility visibility,
            EStatus status,
            UUID authorId,
            Pageable pageable) {
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        String contestStatusStr = (contestStatus != null) ? contestStatus.name() : null;
        return contestRepository
                .searchAdminContests(searchKeyword, ruleType, contestStatusStr, visibility, status, authorId, pageable)
                .map(sdo -> {
                    sdo.setContestStatus(contestMapper.getRealTimeStatus(sdo.getStartTime(), sdo.getEndTime()));
                    return sdo;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestAdminSdo getAdminContestById(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestAdminSdo sdo = contestMapper.toAdminSdo(contest);
        sdo.setParticipantCount(contestParticipationRepository.countByContestContestKey(contest.getContestKey()));
        return sdo;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestAdminSdo createContest(CreateContestSdi req, UUID authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (contestRepository.existsByContestKey(req.getContestKey())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contest Key already exists.");
        }

        if (req.getFormat() == ContestFormat.WINDOWED) {
            if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Windowed contests must have a valid duration.");
            }
        } else {
            req.setDurationMinutes(null);
        }

        Contest contest = contestMapper.toEntity(req);
        if (contest.getVisibility() == ContestVisibility.PRIVATE && contest.getPassword() != null && !contest.getPassword().isEmpty()) {
            contest.setPassword(passwordEncoder.encode(contest.getPassword()));
        } else {
            contest.setPassword(null);
        }
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
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        if (contest.getStatus() == EStatus.DELETED) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        validateContestStatusConfig(contest, false, req);

        if (!contest.getContestKey().equals(req.getContestKey()) && contestRepository.existsByContestKey(req.getContestKey())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Contest Key already exists.");
        }

        if (req.getVisibility() == ContestVisibility.PUBLIC) {
            req.setPassword(null);
        } else if (req.getPassword() != null && !req.getPassword().isBlank()) {
            req.setPassword(passwordEncoder.encode(req.getPassword()));
        } else {
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
        log.info("Updated contest: {}", updated.getContestKey());

        ContestAdminSdo sdo = contestMapper.toAdminSdo(updated);
        sdo.setParticipantCount(contestParticipationRepository.countByContestContestKey(updated.getContestKey()));
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

        validateContestStatusConfig(contest, true, null);

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

        validateContestStatusConfig(contest, true, null);

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

        validateContestStatusConfig(contest, true, null);

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

        List<ContestProblemSdo> existing = contestProblemRepository.findByContestKeyOrderBySortOrderAsc(contest.getContestKey());
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

        validateContestStatusConfig(contest, true, null);

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
        
        validateContestStatusConfig(contest, true, null);

        List<ContestProblem> existingProblems = contestProblemRepository.findByContestId(contestId);
        Map<UUID, ContestProblem> problemMap = existingProblems.stream()
                .collect(Collectors.toMap(cp -> cp.getProblem().getId(), cp -> cp));

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
                if (!keys.isEmpty()) {
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
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        return contestProblemRepository.findByContestKeyOrderBySortOrderAsc(contest.getContestKey());
    }

    // ==================================================================== //
    // Whitelist
    
    @Transactional
    @Override
    public void saveContestWhitelist(UUID contestId, List<com.kma.ojcore.dto.request.contests.ContestWhitelistItemSdi> items) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));
        
        // Remove all old emails
        contestWhitelistRepository.deleteByContestId(contestId);
        
        // Insert new emails
        if (items != null && !items.isEmpty()) {
            List<com.kma.ojcore.entity.ContestWhitelist> entities = items.stream()
                .filter(item -> item != null && item.getEmail() != null && !item.getEmail().trim().isEmpty())
                .map(item -> com.kma.ojcore.entity.ContestWhitelist.builder()
                        .contest(contest)
                        .email(item.getEmail().trim().toLowerCase())
                        .fullName(item.getFullName())
                        .phoneNumber(item.getPhoneNumber())
                        .note(item.getNote())
                        .build())
                .toList();
            contestWhitelistRepository.saveAll(entities);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<com.kma.ojcore.dto.response.contests.ContestWhitelistItemSdo> getContestWhitelist(UUID contestId) {
        return contestWhitelistRepository.findByContestId(contestId).stream()
                .map(entity -> com.kma.ojcore.dto.response.contests.ContestWhitelistItemSdo.builder()
                        .email(entity.getEmail())
                        .fullName(entity.getFullName())
                        .phoneNumber(entity.getPhoneNumber())
                        .note(entity.getNote())
                        .build())
                .toList();
    }

    // ==================================================================== //
    // Participants

    @Transactional(readOnly = true)
    @Override
    public Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword,
            Boolean isDisqualified, Pageable pageable) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        String escapedKeyword = EscapeHelper.escapeLike(keyword);
        return contestParticipationRepository.searchParticipants(contest.getContestKey(), escapedKeyword, isDisqualified, pageable);
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

    @Override
    @Transactional(readOnly = true)
    public ContestLeaderboardPageSdo getContestLeaderboard(String contestKey, EStatus status, Pageable pageable, boolean bypassVisibility) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, status)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));
        return getContestLeaderboardInternal(contest, pageable, bypassVisibility);
    }

    private ContestLeaderboardPageSdo getContestLeaderboardInternal(Contest contest, Pageable pageable, boolean bypassVisibility) {
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
        String redisKey = String.format("%s:%s:PAGE:%d:SIZE:%d%s", leaderboardPrefix,
                contest.getContestKey(), pageable.getPageNumber(), pageable.getPageSize(),
                bypassVisibility ? ":ADMIN" : "");

        try {
            String cachedLeaderboard = redisTemplate.opsForValue().get(redisKey);
            if (cachedLeaderboard != null) {
                return objectMapper.readValue(cachedLeaderboard, ContestLeaderboardPageSdo.class);
            }
        } catch (Exception e) {
            log.error("Redis Cache Error for Leaderboard: {}", e.getMessage());
        }

        // ==========================================
        // 3. NẾU REDIS TRỐNG -> CHỌC XUỐNG DB
        // ==========================================
        Page<ContestParticipationRepository.ContestLeaderboardProjection> nativePage = contestParticipationRepository.getLeaderboardNative(contest.getId(), pageable);

        List<ContestLeaderboardSdo> sdoContent = nativePage.getContent().stream().map(p -> {
            ContestLeaderboardSdo sdo = new ContestLeaderboardSdo();
            sdo.setUserId(UuidHelper.getUuidFromBytes(p.getUserId()));
            sdo.setUsername(p.getUsername());
            sdo.setScore(p.getScore());
            sdo.setPenalty(p.getPenalty());
            sdo.setRank(p.getRank());
            return sdo;
        }).collect(Collectors.toList());

        Page<ContestLeaderboardSdo> page = new PageImpl<>(sdoContent, pageable, nativePage.getTotalElements());

        List<ContestProblem> contestProblems = contestProblemRepository.findByContestId(contest.getId());

        if (!page.isEmpty()) {
            List<UUID> userIds = page.getContent().stream().map(ContestLeaderboardSdo::getUserId).collect(Collectors.toList());
            
            // Lấy trạng thái trung gian (Intermediate State) của tất cả user trong Page này
            List<ContestParticipationProblem> matrixRecords = cppRepository.findByParticipationUserIdInAndContestProblemContestId(userIds, contest.getId());

            // Nhóm dữ liệu: UserId -> (DisplayId -> ContestParticipationProblem)
            Map<UUID, Map<String, ContestParticipationProblem>> cppByUserAndDisplayId = matrixRecords.stream()
                    .collect(Collectors.groupingBy(cpp -> cpp.getParticipation().getUser().getId(),
                            Collectors.toMap(cpp -> cpp.getContestProblem().getDisplayId(), cpp -> cpp)));

            for (ContestLeaderboardSdo lb : page) {
                Map<String, ContestParticipationProblem> userCpps = cppByUserAndDisplayId.getOrDefault(lb.getUserId(), Collections.emptyMap());

                for (ContestProblem cp : contestProblems) { // Vòng lặp này đã duyệt qua toàn bộ problems của contest rồi
                    String displayId = cp.getDisplayId();
                    ContestParticipationProblem cpp = userCpps.get(displayId);

                    ContestProblemResultSdo result = new ContestProblemResultSdo();
                    result.setProblemId(cp.getProblem().getId());
                    result.setDisplayId(displayId);
                    
                    if (cpp != null) {
                        result.setTries(cpp.getFailedAttempts());
                        result.setIsAc(cpp.getIsAc());
                        result.setScore(cpp.getMaxScore());
                        result.setPenalty(cpp.getPenalty() == Long.MAX_VALUE ? 0L : cpp.getPenalty());
                    } else {
                        result.setTries(0);
                        result.setIsAc(false);
                        result.setScore(0.0);
                        result.setPenalty(0L);
                    }
                    
                    lb.getProblemResults().put(displayId, result);
                }
            }
        }

        List<ContestProblemSdo> problemSdos = contestProblems.stream()
                .map(cp -> ContestProblemSdo.builder()
                        .id(cp.getId())
                        .problemId(cp.getProblem().getId())
                        .problemSlug(cp.getProblem().getSlug())
                        .originalTitle(cp.getProblem().getTitle())
                        .displayId(cp.getDisplayId())
                        .points(cp.getPoints())
                        .sortOrder(cp.getSortOrder())
                        .build())
                .sorted(Comparator.comparing(ContestProblemSdo::getSortOrder))
                .toList();
 
        ContestLeaderboardPageSdo response = ContestLeaderboardPageSdo.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .problems(problemSdos)
                .build();

        try {
            String jsonToCache = objectMapper.writeValueAsString(response);
 
            // TTL = 5 giây là quá đủ để chặn hàng vạn request F5 cùng lúc
            redisTemplate.opsForValue().set(redisKey, jsonToCache, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to save Leaderboard to Redis: {}", e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public ContestLeaderboardPageSdo getContestLeaderboardForAdmin(UUID contestId, Pageable pageable) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        return getContestLeaderboardInternal(contest, pageable, true);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SubmissionBasicSdo> getMyContestSubmissions(String contestKey, UUID userId, UUID problemId, Pageable pageable) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        // Cha(^.n xem ne^'u chu'a da(ng ky'
        if (!contestParticipationRepository.existsByContestContestKeyAndUserId(contestKey, userId)) {
            throw new BusinessException(ErrorCode.NOT_REGISTERED, "You are not registered for this contest.");
        }

        // Ne^'u contest da( ket thu'c, kie^?m tra resourceVisibility
        ContestStatus submTimeStatus = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());
        if (submTimeStatus == ContestStatus.ENDED) {
            com.kma.ojcore.enums.ContestResourceVisibility rv = contest.getResourceVisibility();
            if (rv == com.kma.ojcore.enums.ContestResourceVisibility.ONLY_DURING) {
                throw new BusinessException(ErrorCode.RESOURCE_ACCESS_DENIED,
                        "Ky thi da ket thuc. Lich su nop bai da duoc bao mat.");
            }
        }

        return submissionRepository.findMyContestSubmissions(contestKey, userId, problemId, pageable);
    }

    // ==================================================================== //
    // EXPORT
    @Transactional(readOnly = true)
    @Override
    public byte[] exportContestResults(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        // Get all participants using a large page size
        ContestLeaderboardPageSdo leaderboard = getContestLeaderboardInternal(contest, PageRequest.of(0, 100000), true);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM for UTF-8 Excel compat
        csv.append("Rank,Username,Score,Penalty");

        for (ContestProblemSdo prob : leaderboard.getProblems()) {
            csv.append(",").append(prob.getDisplayId()).append(" (Score)");
        }
        csv.append("\n");

        for (ContestLeaderboardSdo row : leaderboard.getContent()) {
            csv.append(row.getRank() != null ? row.getRank() : "").append(",");
            csv.append(row.getUsername() != null ? row.getUsername() : "").append(",");
            csv.append(row.getScore() != null ? row.getScore() : 0).append(",");
            csv.append(row.getPenalty() != null ? row.getPenalty() : 0);

            for (ContestProblemSdo prob : leaderboard.getProblems()) {
                ContestProblemResultSdo pr = row.getProblemResults().get(prob.getDisplayId());
                if (pr != null) {
                    csv.append(",").append(pr.getScore());
                } else {
                    csv.append(",0");
                }
            }
            csv.append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
                .searchAdminContests(searchKeyword, ruleType, contestStatusStr, ContestVisibility.PUBLIC, EStatus.ACTIVE, null, pageable)
                .map(contest -> {
                    contest.setContestStatus(
                            contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime()));
                    return contest;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public ContestDetailSdo getContestDetailsForUser(String contestKey, UUID userId) {
        Contest contest = contestRepository.findContestWithAuthorByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        // ============================================================
        // WHITELIST GATE: Private contest -> chan hoan toan neu khong
        // co trong whitelist (chua dang ky va khong co trong ds)
        // ============================================================
        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED,
                        "This is a private contest. Please log in to continue.");
            }
            boolean isParticipant = contestParticipationRepository
                    .existsByContestContestKeyAndUserId(contestKey, userId);
            if (!isParticipant) {
                String email = userRepository.findById(userId)
                        .map(com.kma.ojcore.entity.User::getEmail).orElse(null);
                boolean isWhitelisted = email != null
                        && contestWhitelistRepository.existsByContestIdAndEmail(contest.getId(), email);
                if (!isWhitelisted) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED,
                            "You are not authorized to view this private contest.");
                }
            }
        }
        // ============================================================

        ContestDetailSdo sdo = contestMapper.toDetailSdo(contest);
        sdo.setParticipantCount(contestParticipationRepository.countByContestContestKey(contestKey));

        // 1. Bom serverTime vao cho Frontend dong bo dong ho chong cheat
        sdo.setServerTime(java.time.LocalDateTime.now());

        // 2. Xu ly lay thong tin User (Gop lam 1 cau query duy nhat)
        if (userId != null) {
            ContestParticipation participation = contestParticipationRepository
                    .findByContestContestKeyAndUserId(contestKey, userId).orElse(null);
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
    public void registerContest(String contestKey, UUID userId, RegisterContestSdi req) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
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
            if (req == null || req.getPassword() == null || !passwordEncoder.matches(req.getPassword(), contest.getPassword())) {
                throw new BusinessException(ErrorCode.INCORRECT_PASSWORD);
            }
            checkWhitelistIfPrivate(contest, userId);
        }

        if (contestParticipationRepository.existsByContestIdAndUserId(contest.getId(), userId)) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
        }

        User user = userRepository.getReferenceById(userId);

        ContestParticipation participation = ContestParticipation.builder()
                .contest(contest)
                .user(user)
                .isRegistered(true)
                .build();

        contestParticipationRepository.save(participation);
        log.info("User {} registered for contest {}", userId, contest.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public List<ContestProblemSdo> getContestProblemsForUser(String contestKey, UUID userId) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
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
            checkWhitelistIfPrivate(contest, userId);
            
            ContestParticipation participation = contestParticipationRepository.findByContestContestKeyAndUserId(contestKey, userId)
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
                
                if (contest.getVisibility() == ContestVisibility.PRIVATE) {
                    String email = userRepository.findById(userId).map(User::getEmail).orElse("");
                    contestWhitelistRepository.deleteByContestIdAndEmail(contest.getId(), email);
                }
            }
        }

        // ========================================================
        // 3. NẾU timeStatus == ENDED → kiểm tra resourceVisibility
        // ========================================================
        if (timeStatus == ContestStatus.ENDED) {
            com.kma.ojcore.enums.ContestResourceVisibility rv = contest.getResourceVisibility();
            if (rv == com.kma.ojcore.enums.ContestResourceVisibility.ONLY_DURING) {
                throw new BusinessException(ErrorCode.RESOURCE_ACCESS_DENIED,
                        "Ky thi da ket thuc. De thi va bai lam da duoc bao mat.");
            }
            // ALWAYS_VISIBLE -> upsolving, tiep tuc xuong
        }
        List<ContestProblemSdo> contestProblems = contestProblemRepository.findByContestKeyOrderBySortOrderAsc(contestKey);

        // Đoạn lấy Verdict này vẫn chạy an toàn ngay cả khi User chưa đăng ký
        // (Vì findVerdictsByContestAndUser sẽ trả về list rỗng, không gây lỗi)
        List<SubmissionRepository.ProblemVerdictProjection> userSubmissions =
                submissionRepository.findVerdictsByContestAndUser(contest.getId(), userId);

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
    public Page<ContestParticipantPublicSdo> getPublicContestParticipants(String contestKey,
                                                                          String keyword,
                                                                          Pageable pageable) {
        if (!contestRepository.existsByContestKeyAndStatus(contestKey, EStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.CONTEST_NOT_FOUND);
        }

        String escapedKeyword = EscapeHelper.escapeLike(keyword);

        return contestParticipationRepository.searchPublicParticipants(contestKey, escapedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SubmissionBasicSdo> getAdminContestSubmissions(UUID contestId, Pageable pageable) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found."));

        return submissionRepository.findAllContestSubmissions(contest.getContestKey(), pageable);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ContestParticipationSdo startContest(String contestKey, UUID userId) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND, "Contest not found or not active."));

        // Sửa đoạn này:
        ContestParticipation participation = contestParticipationRepository.findByContestContestKeyAndUserId(contest.getContestKey(), userId)
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
        log.info("User {} started contest {}. Session ends at {}", userId, contest.getId(), participation.getEndTime());

        return contestMapper.toParticipationSdo(participation);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void finishContest(String contestKey, UUID userId) {
        Contest contest = contestRepository.findByContestKeyAndStatus(contestKey, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTEST_NOT_FOUND));

        ContestParticipation participation = contestParticipationRepository.findByContestIdAndUserId(contest.getId(), userId)
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

        // Xóa khỏi whitelist để chặn truy cập vĩnh viễn (nếu là Private)
        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            String email = participation.getUser().getEmail();
            contestWhitelistRepository.deleteByContestIdAndEmail(contest.getId(), email);
        }

        log.info("User {} finished contest {} early.", userId, contest.getId());
    }

    private void checkWhitelistIfPrivate(Contest contest, UUID userId) {
        if (contest.getVisibility() == ContestVisibility.PRIVATE) {
            String email = userRepository.findById(userId).map(User::getEmail)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User email not found."));
            if (!contestWhitelistRepository.existsByContestIdAndEmail(contest.getId(), email)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Your email is not in the authorized whitelist, or you have already finished the contest.");
            }
        }
    }

    private void validateContestStatusConfig(Contest contest, boolean isCriticalAction, UpdateContestSdi req) {
        ContestStatus status = contestMapper.getRealTimeStatus(contest.getStartTime(), contest.getEndTime());

        if (status == ContestStatus.ENDED) {
            if (isCriticalAction) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot perform critical modifications (e.g. problems, points) on an ended contest.");
            }
            if (req != null) {
                if (contest.getFormat() != req.getFormat()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change contest format after it has ended.");
                }
                if (contest.getRuleType() != req.getRuleType()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change rule type after it has ended.");
                }
                if (!contest.getStartTime().equals(req.getStartTime())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change start time after the contest has ended.");
                }
                if (req.getEndTime() != null && !contest.getEndTime().equals(req.getEndTime())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change end time after the contest has ended.");
                }
            }
        }

        if (status == ContestStatus.ONGOING) {
            if (isCriticalAction) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot perform critical modifications (e.g. problems, points) while the contest is ongoing.");
            }
            if (req != null) {
                if (contest.getFormat() != req.getFormat()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change contest format while it is ongoing.");
                }
                if (contest.getRuleType() != req.getRuleType()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change rule type while it is ongoing.");
                }
                if (!contest.getStartTime().equals(req.getStartTime())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cannot change start time while the contest is ongoing.");
                }
                if (req.getEndTime() != null && req.getEndTime().isBefore(contest.getEndTime())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Can only extend the end time while the contest is ongoing.");
                }
            }
        }
    }
}

