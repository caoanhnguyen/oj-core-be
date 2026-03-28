package com.kma.ojcore.controller.contests;

import com.kma.ojcore.dto.request.contests.RegisterContestSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.contests.ContestBasicSdo;
import com.kma.ojcore.dto.response.contests.ContestDetailSdo;
import com.kma.ojcore.dto.response.contests.ContestParticipantPublicSdo;
import com.kma.ojcore.dto.response.contests.ContestProblemSdo;
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
}