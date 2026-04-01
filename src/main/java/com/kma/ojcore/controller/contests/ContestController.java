package com.kma.ojcore.controller.contests;

import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.ContestService;
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
@RequestMapping("${app.api.prefix}/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @GetMapping("/my-active")
    public ApiResponse<List<MyActiveContestSdo>> getMyActiveContests(@AuthenticationPrincipal UserPrincipal currentUser) {

        List<MyActiveContestSdo> activeContests = contestService.getMyActiveContests(currentUser.getId());

        return ApiResponse.<List<MyActiveContestSdo>>builder()
                .status(org.springframework.http.HttpStatus.OK.value())
                .message("Fetched active contests successfully")
                .data(activeContests)
                .build();
    }


    @GetMapping("/{id}/leaderboard")
    public ApiResponse<Page<ContestLeaderboardSdo>> getLeaderboard(@PathVariable UUID id,
                                                                   Pageable pageable) {

        return ApiResponse.<Page<ContestLeaderboardSdo>>builder()
                .status(200)
                .message("Fetched leaderboard successfully")
                .data(contestService.getContestLeaderboard(id, pageable))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<ContestBasicSdo>> getContests(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) RuleType ruleType,
                                                          @RequestParam(required = false) ContestStatus contestStatus,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @SortDefault(sort = "startTime", direction = Sort.Direction.DESC) Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.<Page<ContestBasicSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contests successfully.")
                .data(contestService.getContestsForUser(keyword, ruleType, contestStatus, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContestDetailSdo> getContestDetails(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UUID userId = (userPrincipal != null) ? userPrincipal.getId() : null;

        return ApiResponse.<ContestDetailSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest details successfully.")
                .data(contestService.getContestDetailsForUser(id, userId))
                .build();
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> registerContest(
            @PathVariable UUID id,
            @RequestBody(required = false) RegisterContestSdi request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        contestService.registerContest(id, userPrincipal.getId(), request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Successfully registered for the contest.")
                .build();
    }

    @GetMapping("/{id}/problems")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ContestProblemSdo>> getContestProblems(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ApiResponse.<List<ContestProblemSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest problems successfully.")
                .data(contestService.getContestProblemsForUser(id, userPrincipal.getId()))
                .build();
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<Page<ContestParticipantPublicSdo>> getPublicParticipants(
                                @PathVariable UUID id,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @SortDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.<Page<ContestParticipantPublicSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched public participants successfully")
                .data(contestService.getPublicContestParticipants(id, keyword, pageable))
                .build();
    }

    @GetMapping("/{id}/submissions/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<SubmissionBasicSdo>> getMySubmissions(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID problemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {

        return ApiResponse.<Page<SubmissionBasicSdo>>builder()
                .status(200)
                .message("Fetched your contest submissions successfully")
                .data(contestService.getMyContestSubmissions(id, userPrincipal.getId(), problemId, pageable))
                .build();
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ContestParticipationSdo> startContest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ApiResponse.<ContestParticipationSdo>builder()
                .status(200)
                .message("Contest session started successfully.")
                .data(contestService.startContest(id, userPrincipal.getId()))
                .build();
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> finishContest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        contestService.finishContest(id, userPrincipal.getId());
        return ApiResponse.<String>builder()
                .status(200)
                .message("Contest session finished successfully.")
                .build();
    }
}