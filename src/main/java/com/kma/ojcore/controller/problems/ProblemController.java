package com.kma.ojcore.controller.problems;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.simpleframework.xml.core.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Validate
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
public class ProblemController {

    private final SubmissionService submissionService;

    @GetMapping("/{id}/statistics")
    public ApiResponse<?> getStatisticsForAdmin(
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
}
