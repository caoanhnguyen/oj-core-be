package com.kma.ojcore.controller.submissions;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ProblemStatisticSdo;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/submissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminSubmissionController {

    private final SubmissionService submissionService;


    /**
     * Lấy danh sách submission cho ADMIN. Mặc định lấy submission của bất kì problem nào, không quan tâm status.
     * @param problemId
     * @param userId
     * @param submissionVerdict
     * @param username
     * @param page
     * @param size
     * @param sort
     * @return
     */
    @GetMapping
    public ApiResponse<?> getSubmissions(
            @RequestParam(required = false) UUID problemId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) SubmissionVerdict submissionVerdict,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PageableDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submissions retrieved successfully")
                .data(submissionService.getSubmissions(problemId, userId, submissionVerdict, username, null, null, pageable))
                .build();
    }

}
