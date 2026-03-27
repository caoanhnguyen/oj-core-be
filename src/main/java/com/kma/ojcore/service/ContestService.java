package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.contests.ContestAdminSdo;
import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.dto.response.contests.ContestDetailSdo;
import com.kma.ojcore.dto.response.contests.ContestProblemSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.RuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ContestService {
    // ADMIN

    Page<ContestAdminSdo> searchAdminContests(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestAdminSdo createContest(CreateContestSdi req, UUID authorId);

    ContestAdminSdo updateContest(UUID contestId, UpdateContestSdi req);

    void deleteContest(UUID contestId);

    ContestAdminSdo getAdminContestById(UUID contestId);

    void addProblemToContest(UUID contestId, AddContestProblemSdi req);

    void removeProblemFromContest(UUID contestId, UUID problemId);

    List<ContestProblemSdo> getContestProblemsForAdmin(UUID contestId);

    // USER
    Page<ContestBasicSdo> getContestsForUser(String keyword, RuleType ruleType, ContestStatus contestStatus, Pageable pageable);

    ContestDetailSdo getContestDetailsForUser(UUID contestId, UUID userId);

    void registerContest(UUID contestId, UUID userId, RegisterContestSdi req);

    List<ContestProblemSdo> getContestProblemsForUser(UUID contestId, UUID userId);
}