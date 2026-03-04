package com.kma.ojcore.controller.problems;

import com.kma.ojcore.dto.request.problems.CreateProblemSdi;
import com.kma.ojcore.dto.request.problems.ProblemFilter;
import com.kma.ojcore.dto.request.problems.UpdateProblemSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ProblemDetailsSdo;
import com.kma.ojcore.dto.response.problems.ProblemResponse;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.simpleframework.xml.core.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Problems Controller:
 * CRUD các bài tập, quản lý test case, template code, driver code
 */
@RestController
@RequestMapping("/api/admin/problems")
@RequiredArgsConstructor
@Validate
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProblemDetailsSdo> createProblem(@Valid @RequestBody CreateProblemSdi request) throws BadRequestException {
        ProblemDetailsSdo result = problemService.createProblem(request);
        return ApiResponse.<ProblemDetailsSdo>builder()
                .status(HttpStatus.CREATED.value())
                .message("Problem created successfully")
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
        ProblemDetailsSdo result = problemService.  getProblemBySlug(slug);
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
            @RequestParam(required = false) EStatus status,
            @RequestParam(required = false) ProblemStatus problemStatus,
            @RequestParam(required = false) List<String> topicSlugs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort) {

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProblemResponse> result = problemService.getProblems(keyword, difficulty, status, problemStatus, topicSlugs, pageable);

        return ApiResponse.<Page<ProblemResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Get problems successfully")
                .data(result)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProblemDetailsSdo> updateProblem(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateProblemSdi request) throws BadRequestException {
        ProblemDetailsSdo result = problemService.updateProblem(id, request);
        return ApiResponse.<ProblemDetailsSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Problem updated successfully")
                .data(result)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProblem(@PathVariable UUID id) {
        problemService.deleteProblem(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Problem deleted successfully")
                .build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> restoreProblem(@PathVariable UUID id) {
        problemService.restoreProblem(id);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Problem restored successfully")
                .build();
    }
}
