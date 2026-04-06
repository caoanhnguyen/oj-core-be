package com.kma.ojcore.controller.submissions;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

import com.kma.ojcore.dto.request.submissions.RejudgeSdi;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("${app.api.prefix}/admin/submissions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
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
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @SortDefault(sort = "createdDate", direction = Sort.Direction.DESC) Sort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, sort);

        List<SubmissionVerdict> allowedVerdicts = SubmissionVerdict.getAllVerdicts();
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submissions retrieved successfully")
                .data(submissionService.getSubmissions(problemId, userId, submissionVerdict, keyword, null, null, allowedVerdicts, false, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getSubmissionResult(@PathVariable UUID id) {
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submission result retrieved successfully")
                .data(submissionService.getSubmissionBasicInfo(id))
                .build();
    }

    @PostMapping("/rejudge")
    public ApiResponse<?> rejudgeSubmissions(@RequestBody RejudgeSdi request) {
        submissionService.rejudgeSubmissions(request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submissions have been queued for rejudging.")
                .build();
    }
}
