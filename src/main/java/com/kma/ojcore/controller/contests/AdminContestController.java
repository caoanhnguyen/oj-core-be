package com.kma.ojcore.controller.contests;

import com.kma.ojcore.dto.request.contests.AddContestProblemSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestProblemSdi;
import com.kma.ojcore.dto.request.contests.CreateContestSdi;
import com.kma.ojcore.dto.request.contests.UpdateContestSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.EStatus;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/contests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Validated
public class AdminContestController {

    private final ContestService contestService;

    // ====================================================
    // CONTEST

    @GetMapping
    public ApiResponse<?> getContests(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) RuleType ruleType,
                                      @RequestParam(required = false) ContestStatus contestStatus,
                                      @RequestParam(required = false) ContestVisibility visibility,
                                      @RequestParam(required = false) EStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @SortDefault Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<Page<ContestBasicSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contests successfully.")
                .data(contestService.searchAdminContests(keyword, ruleType, contestStatus, visibility, status, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContestAdminSdo> getContestDetails(@PathVariable UUID id) {
        return ApiResponse.<ContestAdminSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest details successfully.")
                .data(contestService.getAdminContestById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<ContestAdminSdo> createContest(@Valid @RequestBody CreateContestSdi request,
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

    @PostMapping("/{id}/restore")
    public ApiResponse<?> restoreContest(@PathVariable UUID id) {
        contestService.restoreContest(id);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Contest restored successfully.")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> softDeleteContest(@PathVariable UUID id) {
        contestService.softDeleteContest(id);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Contest deleted successfully.")
                .build();
    }

    @PatchMapping("/{id}/visibility")
    public ApiResponse<?> toggleVisibility(@PathVariable UUID id) {
        contestService.togglePublishStatus(id);
        return ApiResponse.<String>builder()
                .status(200)
                .message("Contest visibility updated successfully.")
                .build();
    }

    // ====================================================
    // PROBLEMS CONTEST

    @GetMapping("/{id}/problems")
    public ApiResponse<List<ContestProblemSdo>> getContestProblems(@PathVariable UUID id) {
        return ApiResponse.<List<ContestProblemSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest problems successfully.")
                .data(contestService.getContestProblemsForAdmin(id))
                .build();
    }

    @PostMapping("/{id}/problems/bulk-add")
    public ApiResponse<?> addProblems(@PathVariable UUID id,
                                      @RequestBody List<AddContestProblemSdi> requests) {
        contestService.addProblemsToContest(id, requests);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Added problems successfully.")
                .build();
    }

    @PutMapping("/{id}/problems/bulk-update")
    public ApiResponse<?> updateProblems(@PathVariable UUID id,
                                         @RequestBody List<UpdateContestProblemSdi> requests) {
        contestService.updateProblemsInContest(id, requests);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Updated problems successfully.")
                .build();
    }

    @DeleteMapping("/{id}/problems/bulk-remove")
    public ApiResponse<?> removeProblems(@PathVariable UUID id,
                                         @RequestBody List<UUID> problemIds) {
        contestService.removeProblemsFromContest(id, problemIds);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Removed problems successfully.")
                .build();
    }

    // ====================================================
    // PARTICIPANTS

    @GetMapping("/{id}/participants")
    public ApiResponse<Page<ContestParticipationSdo>> getParticipants(
            @PathVariable UUID id,
              @RequestParam(value = "keyword", required = false) String keyword,
              @RequestParam(value = "isDisqualified", required = false) Boolean isDisqualified,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size,
              @SortDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort
    ) {

        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<Page<ContestParticipationSdo>>builder()
                .status(200)
                .message("Fetched participants successfully")
                .data(contestService.searchContestParticipants(id, keyword, isDisqualified, pageable))
                .build();
    }

    @PostMapping("/{id}/participants/bulk-ban")
    public ApiResponse<?> banUsers(@PathVariable UUID id,
                                   @RequestBody List<UUID> userIds) {

        contestService.disqualifyUsers(id, userIds);
        return ApiResponse.<String>builder()
                .status(200)
                .message("Users disqualified successfully.")
                .build();
    }

    @PostMapping("/{id}/participants/bulk-unban")
    public ApiResponse<?> unbanUsers(@PathVariable UUID id,
                                     @RequestBody List<UUID> userIds) {
        contestService.requalifyUsers(id, userIds);
        return ApiResponse.<String>builder()
                .status(200)
                .message("Users requalified successfully")
                .build();
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<Page<SubmissionBasicSdo>> getContestSubmissions(
            @PathVariable UUID id,
            Pageable pageable) {

        return ApiResponse.<Page<SubmissionBasicSdo>>builder()
                .status(200)
                .message("Fetched all contest submissions successfully")
                .data(contestService.getAdminContestSubmissions(id, pageable))
                .build();
    }

    @GetMapping("/{id}/leaderboard")
    public ApiResponse<Page<ContestLeaderboardSdo>> getLeaderboard(@PathVariable UUID id,
                                                                   Pageable pageable) {

        return ApiResponse.<Page<ContestLeaderboardSdo>>builder()
                .status(200)
                .message("Fetched leaderboard successfully")
                .data(contestService.getContestLeaderboard(id, null, pageable))
                .build();
    }
}