package com.kma.ojcore.controller.submissions;

import com.kma.ojcore.dto.request.submissions.RunCodeSubmitDto;
import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.submissions.RunCodeResponse;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.ProblemStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.RunCodeService;
import com.kma.ojcore.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("${app.api.prefix}/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RunCodeService runCodeService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> submitCode(@Valid @RequestBody SubmissionSdi request,
                                     @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UUID submissionId = submissionService.submitCode(request, userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Code submitted successfully")
                .data(submissionId)
                .build();
    }

    @PostMapping("/run_code")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> runCode(@Valid @RequestBody RunCodeSubmitDto request) {
        UUID runCodeToken = runCodeService.sendToJudge(request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Code submitted for execution successfully")
                .data(runCodeToken)
                .build();

    }

    /**
     * Lấy kết quả submission. Chỉ lấy được nếu submission thuộc về user hoặc user có role ADMIN/MODERATOR. Kết quả trả về chỉ là thông tin cơ bản (không bao gồm test case detail).
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @PreAuthorize("@submissionSecurity.isSubmissionOwnerOrAdmin(#id, authentication)")
    public ApiResponse<?> getSubmissionResult(@PathVariable UUID id) {
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submission result retrieved successfully")
                .data(submissionService.getSubmissionBasicInfo(id))
                .build();
    }

    @GetMapping("/run_code/result/{token}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getRunCodeResult(@PathVariable UUID token) {
        RunCodeResponse response = submissionService.getRunCodeResult(token);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Run code result retrieved successfully")
                .data(response)
                .build();
    }

    /**
     * Lấy danh sách submission cho user. Mặc định chỉ lấy submissions của problem có trạng thái PUBLISHED và có status ACTIVE.
     * @param problemId
     * @param userId
     * @param submissionVerdict
     * @param keyword
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
        List<SubmissionVerdict> allowedVerdicts = SubmissionVerdict.getPublicVerdicts();
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Submissions retrieved successfully")
                .data(submissionService.getSubmissions(problemId, userId, submissionVerdict, keyword, EStatus.ACTIVE, ProblemStatus.PUBLISHED, allowedVerdicts, true, pageable))
                .build();
    }

    @GetMapping("latest_source_code/{problemId}/{languageKey}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getLatestSourceCode(@PathVariable UUID problemId,
                                              @PathVariable String languageKey,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String sourceCode = submissionService.getLatestSubmissionCode(problemId, userPrincipal.getId(), languageKey);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Latest source code retrieved successfully")
                .data(sourceCode)
                .build();
    }
}
