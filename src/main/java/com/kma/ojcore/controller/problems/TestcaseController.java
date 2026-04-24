package com.kma.ojcore.controller.problems;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/testcases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
public class TestcaseController {

    private final TestcaseService testcaseService;

    @PostMapping("/upload/{problemId}")
    public ApiResponse<?> uploadTestcases(@PathVariable UUID problemId, @RequestParam("file") MultipartFile file) throws IOException {
        testcaseService.processAndUploadTestcases(problemId, file);
        return ApiResponse.builder()
                .status(200)
                .message("Testcases uploaded successfully")
                .build();
    }
}
