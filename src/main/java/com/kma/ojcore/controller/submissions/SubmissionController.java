package com.kma.ojcore.controller.submissions;

import com.kma.ojcore.dto.request.submissions.SubmissionSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ApiResponse<?> submitCode(@Valid @RequestBody SubmissionSdi request) {
        // Lấy ID của user hiện tại từ security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        UUID submissionId = submissionService.submitCode(request, user.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Code submitted successfully")
                .data(submissionId)
                .build();
    }
}
