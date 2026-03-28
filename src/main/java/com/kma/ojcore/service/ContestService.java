package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ContestService {
    // ADMIN

    Page<ContestBasicSdo> searchAdminContests(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestAdminSdo createContest(CreateContestSdi req, UUID authorId);

    ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req);

    ContestAdminSdo getAdminContestById(UUID contestId);

    void addProblemsToContest(UUID contestId, List<AddContestProblemSdi> requests);

    void removeProblemsFromContest(UUID contestId, List<UUID> problemIds);

    List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId);

    void disqualifyUsers(UUID contestId, List<UUID> userId);

    void updateContestVisibility(UUID contestId, boolean isVisible);

    void restoreContest(UUID contestId);

    void softDeleteContest(UUID contestId);

    Page<ContestParticipationSdo> searchContestParticipants(UUID contestId, String keyword, Boolean isDisqualified, Pageable pageable);

    // USER
    Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId);

    void registerContest(UUID contestId, UUID userId, RegisterContestSdi req);

    List<ContestProblemSdo> getContestProblemsForUser(UUID contestId, UUID userId);

    Page<ContestParticipantPublicSdo> getPublicContestParticipants(UUID contestId, String keyword, Pageable pageable);
}