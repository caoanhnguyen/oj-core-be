package com.kma.ojcore.controller;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.entity.TestCase;
import com.kma.ojcore.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems/{problemId}/testcases")
@RequiredArgsConstructor
public class TestCaseController {

        private final TestCaseService testCaseService;

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<TestCase> uploadTestCase(
                        @PathVariable UUID problemId,
                        @RequestParam(required = false) MultipartFile inputFile,
                        @RequestParam(required = false) MultipartFile outputFile,
                        @RequestParam(required = false) MultipartFile illustration,
                        @RequestParam(required = false) String inputData,
                        @RequestParam(required = false) String outputData,
                        @RequestParam(defaultValue = "false") boolean isSample,
                        @RequestParam(defaultValue = "false") boolean isHidden,
                        @RequestParam(required = false) Integer orderIndex) throws IOException {

                TestCase saved = testCaseService.createTestCase(
                                problemId,
                                inputFile,
                                outputFile,
                                illustration,
                                inputData,
                                outputData,
                                isSample,
                                isHidden,
                                orderIndex);

                // Trả trực tiếp entity TestCase sẽ gây LazyInitializationException khi
                // serialize quan hệ Problem.
                // Chỉ trả id để client có thể gọi lại GET /api/problems/{id} (vốn đã có DTO đầy
                // đủ).
                TestCase response = new TestCase();
                response.setId(saved.getId());

                return ApiResponse.<TestCase>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Test case created successfully")
                                .data(response)
                                .build();
        }

        @PostMapping("/batch-zip")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Object> uploadTestCasesBatch(
                        @PathVariable UUID problemId,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(value = "metadata", required = false) String metadata) throws IOException {
                testCaseService.createTestCasesFromZip(problemId, file, metadata);

                return ApiResponse.builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Test cases uploaded successfully")
                                .build();
        }
}
