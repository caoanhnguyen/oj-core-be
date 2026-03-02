package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.enums.ProblemDifficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProblemService {

    ProblemDetailsSdo createProblem(CreateProblemSdi request);

    ProblemDetailsSdo getProblemById(UUID id);

    ProblemDetailsSdo getProblemBySlug(String slug);

    Page<ProblemResponse> getProblems(ProblemDifficulty difficulty,
                                      String keyword,
                                      Pageable pageable);

    ProblemDetailsSdo updateProblem(UUID id, CreateProblemSdi request);

    void deleteProblem(UUID id);
}
