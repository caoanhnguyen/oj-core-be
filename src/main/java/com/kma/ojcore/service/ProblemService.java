package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.request.problems.ProblemFilter;
import com.kma.ojcore.dto.request.problems.UpdateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProblemService {

    ProblemDetailsSdo createProblem(CreateProblemSdi request) throws BadRequestException;

    ProblemDetailsSdo getProblemById(UUID id);

    ProblemDetailsSdo getProblemBySlug(String slug);

    Page<ProblemResponse> getProblems(String keyword,
                                      ProblemDifficulty difficulty,
                                      EStatus status,
                                      ProblemStatus problemStatus,
                                      List<String> topicSlugs,
                                      Pageable pageable);

    ProblemDetailsSdo updateProblem(UUID id, UpdateProblemSdi request) throws BadRequestException;

    void deleteProblem(UUID id);

    void restoreProblem(UUID id);
}
