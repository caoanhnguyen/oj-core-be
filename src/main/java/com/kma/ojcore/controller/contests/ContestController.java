package com.kma.ojcore.controller.contests;

import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.contests.*;
import com.kma.ojcore.dto.response.submissions.SubmissionBasicSdo;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.EStatus;
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
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MyActiveContestSdo>> getMyActiveContests(@AuthenticationPrincipal UserPrincipal currentUser) {

        List<MyActiveContestSdo> activeContests = contestService.getMyActiveContests(currentUser.getId());

        return ApiResponse.<List<MyActiveContestSdo>>builder()
                .status(org.springframework.http.HttpStatus.OK.value())
                .message("Fetched active contests successfully")
                .data(activeContests)
                .build();
    }


    @GetMapping("/{contestKey}/leaderboard")
    public ApiResponse<ContestLeaderboardPageSdo> getLeaderboard(@PathVariable String contestKey,
                                                                   Pageable pageable) {
 
        return ApiResponse.<ContestLeaderboardPageSdo>builder()
                .status(200)
                .message("Fetched leaderboard successfully")
                .data(contestService.getContestLeaderboard(contestKey, EStatus.ACTIVE, pageable, false))
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

    @GetMapping("/{contestKey}")
    public ApiResponse<ContestDetailSdo> getContestDetails(
            @PathVariable String contestKey,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UUID userId = (userPrincipal != null) ? userPrincipal.getId() : null;

        return ApiResponse.<ContestDetailSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest details successfully.")
                .data(contestService.getContestDetailsForUser(contestKey, userId))
                .build();
    }

    @PostMapping("/{contestKey}/register")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> registerContest(
            @PathVariable String contestKey,
            @RequestBody(required = false) RegisterContestSdi request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        contestService.registerContest(contestKey, userPrincipal.getId(), request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Successfully registered for the contest.")
                .build();
    }

    @GetMapping("/{contestKey}/problems")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ContestProblemSdo>> getContestProblems(
            @PathVariable String contestKey,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ApiResponse.<List<ContestProblemSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched contest problems successfully.")
                .data(contestService.getContestProblemsForUser(contestKey, userPrincipal.getId()))
                .build();
    }

    @GetMapping("/{contestKey}/participants")
    public ApiResponse<Page<ContestParticipantPublicSdo>> getPublicParticipants(
                                @PathVariable String contestKey,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @SortDefault(sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.<Page<ContestParticipantPublicSdo>>builder()
                .status(HttpStatus.OK.value())
                .message("Fetched public participants successfully")
                .data(contestService.getPublicContestParticipants(contestKey, keyword, pageable))
                .build();
    }

    @GetMapping("/{contestKey}/submissions/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<SubmissionBasicSdo>> getMySubmissions(
            @PathVariable String contestKey,
            @RequestParam(required = false) UUID problemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {

        return ApiResponse.<Page<SubmissionBasicSdo>>builder()
                .status(200)
                .message("Fetched your contest submissions successfully")
                .data(contestService.getMyContestSubmissions(contestKey, userPrincipal.getId(), problemId, pageable))
                .build();
    }

    @PostMapping("/{contestKey}/start")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ContestParticipationSdo> startContest(
            @PathVariable String contestKey,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return ApiResponse.<ContestParticipationSdo>builder()
                .status(200)
                .message("Contest session started successfully.")
                .data(contestService.startContest(contestKey, userPrincipal.getId()))
                .build();
    }

    @PostMapping("/{contestKey}/finish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> finishContest(
            @PathVariable String contestKey,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        contestService.finishContest(contestKey, userPrincipal.getId());
        return ApiResponse.<String>builder()
                .status(200)
                .message("Contest session finished successfully.")
                .build();
    }
}