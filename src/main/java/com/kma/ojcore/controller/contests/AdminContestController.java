package com.kma.ojcore.controller.contests;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.contests.ContestAdminSdo;
import com.kma.ojcore.dto.response.contests.ContestProblemSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.ContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("${app.api.prefix}/admin/contests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminContestController {

    private final ContestService contestService;

    @GetMapping
    public ApiResponse<Page<ContestAdminSdo>> getContests(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) RuleType ruleType,
                                                          @RequestParam(required = false) ContestStatus contestStatus,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @SortDefault(sort = "startTime", direction = Sort.Direction.DESC) Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<Page<ContestAdminSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contests successfully.")
                .data(contestService.searchAdminContests(keyword, ruleType, contestStatus, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContestAdminSdo> getContest(@PathVariable UUID id) {
        return ApiResponse.<ContestAdminSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest details successfully.")
                .data(contestService.getAdminContestById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<ContestAdminSdo> createContest(
            @Valid @RequestBody CreateContestSdi request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.<ContestAdminSdo>builder()
                .status(HttpStatus.CREATED.value())
                .message("Contest created successfully.")
                .data(contestService.createContest(request, userPrincipal.getId()))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ContestAdminSdo> updateContest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContestSdi request) {
        return ApiResponse.<ContestAdminSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Contest updated successfully.")
                .data(contestService.updateContest(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteContest(@PathVariable UUID id) {
        contestService.deleteContest(id);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Contest deleted successfully.")
                .build();
    }

    @GetMapping("/{id}/problems")
    public ApiResponse<List<ContestProblemSdo>> getContestProblems(@PathVariable UUID id) {
        return ApiResponse.<List<ContestProblemSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest problems successfully.")
                .data(contestService.getContestProblemsForAdmin(id))
                .build();
    }

    @PostMapping("/{id}/problems")
    public ApiResponse<?> addProblemToContest(
            @PathVariable UUID id,
            @Valid @RequestBody AddContestProblemSdi request) {
        contestService.addProblemToContest(id, request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Problem added to contest successfully.")
                .build();
    }

    @DeleteMapping("/{id}/problems/{problemId}")
    public ApiResponse<?> removeProblemFromContest(
            @PathVariable UUID id,
            @PathVariable UUID problemId) {
        contestService.removeProblemFromContest(id, problemId);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Problem removed from contest successfully.")
                .build();
    }
}