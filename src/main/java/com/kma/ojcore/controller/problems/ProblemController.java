package com.kma.ojcore.controller.problems;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.enums.*;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.ProblemService;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.simpleframework.xml.core.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Validate
public class ProblemController {

    private final SubmissionService submissionService;
    private final ProblemService problemService;

    @GetMapping("/{id}/statistics")
    public ApiResponse<?> getPublicStatistics(
            @PathVariable UUID id
    ) {
        ProblemStatisticSdo result = submissionService.getProblemStatistics(
                id, SubmissionVerdict.getPublicVerdicts()
        );
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Problem statistics retrieved successfully")
                .data(result)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProblemDetailsSdo> getProblemById(@PathVariable UUID id) {
        ProblemDetailsSdo result = problemService.getProblemById(id);
        return ApiResponse.<ProblemDetailsSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Get problem details successfully")
                .data(result)
                .build();
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ProblemDetailsSdo> getProblemBySlug(@PathVariable String slug) {
        ProblemDetailsSdo result = problemService.getProblemBySlug(slug);
        return ApiResponse.<ProblemDetailsSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Get problem details successfully")
                .data(result)
                .build();
    }

    @GetMapping
    public ApiResponse<Page<ProblemResponse>> getProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProblemDifficulty difficulty,
            @RequestParam(required = false) RuleType ruleType,
            @RequestParam(required = false) List<String> topicSlugs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @SortDefault(sort = "createdDate", direction = Sort.Direction.ASC) Sort sort,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size, sort);
        UUID userId = currentUser != null ? currentUser.getId() : null;
        Page<ProblemResponse> result = problemService.getProblems(keyword, difficulty, ruleType, topicSlugs, EStatus.ACTIVE, ProblemStatus.PUBLISHED, userId, pageable);

        return ApiResponse.<Page<ProblemResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get problems successfully")
                .data(result)
                .build();
    }

    @GetMapping("/solved/count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> countUserSolvedProblems(@AuthenticationPrincipal UserPrincipal currentUser) {
        UUID userId = currentUser.getId();
        long count = problemService.countUserProblemsByUserIdAndState(userId, UserProblemState.SOLVED);
        return ApiResponse.<Long>builder()
                .status(HttpStatus.OK.value())
                .message("Count solved problems successfully")
                .data(count)
                .build();
    }
}
