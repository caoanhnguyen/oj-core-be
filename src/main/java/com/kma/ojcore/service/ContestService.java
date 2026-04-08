package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ContestService {

    // ADMIN

    Page<ContestBasicSdo> searchAdminContests(String keyword, RuleType ruleType, ContestStatus contestStatus, ContestVisibility visibility, EStatus status, Pageable pageable);

    ContestAdminSdo getAdminContestById(UUID contestId);

    ContestAdminSdo createContest(CreateContestSdi req, UUID authorId);

    ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req);

    void restoreContest(UUID contestId);

    void softDeleteContest(UUID contestId);

    void togglePublishStatus(UUID contestId);

    // Problems Contest

    void addProblemsToContest(UUID contestId, List<AddContestProblemSdi> requests);

    void updateProblemsInContest(UUID contestId, List<UpdateContestProblemSdi> requests);

    void removeProblemsFromContest(UUID contestId, List<UUID> problemIds);

    List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId);

    // Participants

    Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword, Boolean isDisqualified, Pageable pageable);

    void disqualifyUsers(UUID contestId, List<UUID> userId);

    void requalifyUsers(UUID contestId, List<UUID> userIds);

    // Leaderboard & Submissions

    ContestLeaderboardPageSdo getContestLeaderboard(String contestKey, EStatus status, Pageable pageable, boolean bypassVisibility);

    ContestLeaderboardPageSdo getContestLeaderboardForAdmin(UUID contestId, Pageable pageable);

    Page<SubmissionBasicSdo> getAdminContestSubmissions(UUID contestId, Pageable pageable);


    // USER
    List<MyActiveContestSdo> getMyActiveContests(UUID userId);

    Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestDetailSdo getContestDetailsForUser(String contestKey, UUID userId);

    void registerContest(String contestKey, UUID userId, RegisterContestSdi req);

    List<ContestProblemSdo> getContestProblemsForUser(String contestKey, UUID userId);

    Page<ContestParticipantPublicSdo> getPublicContestParticipants(String contestKey, String keyword, Pageable pageable);

    Page<SubmissionBasicSdo> getMyContestSubmissions(String contestKey, UUID userId, UUID problemId, Pageable pageable);

    ContestParticipationSdo startContest(String contestKey, UUID userId);

    void finishContest(String contestKey, UUID userId);
}