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

    Page<ContestLeaderboardSdo> getContestLeaderboard(UUID contestId, EStatus status, Pageable pageable, boolean bypassVisibility);

    Page<SubmissionBasicSdo> getAdminContestSubmissions(UUID contestId, Pageable pageable);


    // USER
    List<MyActiveContestSdo> getMyActiveContests(UUID userId);

    Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId);

    void registerContest(UUID contestId, UUID userId, RegisterContestSdi req);

    List<ContestProblemSdo> getContestProblemsForUser(UUID contestId, UUID userId);

    Page<ContestParticipantPublicSdo> getPublicContestParticipants(UUID contestId, String keyword, Pageable pageable);

    Page<SubmissionBasicSdo> getMyContestSubmissions(UUID contestId, UUID userId, UUID problemId, Pageable pageable);

    ContestParticipationSdo startContest(UUID contestId, UUID userId);

    void finishContest(UUID contestId, UUID userId);
}