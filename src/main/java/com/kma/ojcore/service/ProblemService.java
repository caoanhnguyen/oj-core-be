package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.request.problems.UpdateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.enums.*;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProblemService {

    ProblemDetailsSdo createProblem(CreateProblemSdi request, UUID userId) throws BadRequestException;

    ProblemDetailsSdo getProblemById(UUID id);

    ProblemDetailsSdo getProblemBySlug(String slug);

    /**
     * Get a problem via its contest context after the contest has ended.
     * Allows access to INACTIVE/DRAFT problems if the contest's resourceVisibility = ALWAYS_VISIBLE.
     */
    ProblemDetailsSdo getProblemViaContest(String contestKey, String problemSlug);

    Page<ProblemResponse> getProblems(String keyword,
                                      ProblemDifficulty difficulty,
                                      RuleType ruleType,
                                      List<String> topicSlugs,
                                      EStatus status,
                                      ProblemStatus problemStatus,
                                      UUID userId,
                                      UUID contestId,
                                      Pageable pageable);

    ProblemDetailsSdo updateProblem(UUID id, UpdateProblemSdi request) throws BadRequestException;

    void deleteProblem(UUID id);

    void restoreProblem(UUID id);

    void publishProblem(UUID id);

    long countUserProblemsByUserIdAndState(UUID userId, UserProblemState state);
}
