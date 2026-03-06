package com.kma.ojcore.controller;

import com.kma.ojcore.dto.request.problems.UpdateTestCaseSdi;
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
        public ApiResponse<com.kma.ojcore.dto.response.problems.TestCaseSdo> uploadTestCase(
                        @PathVariable UUID problemId,
                        @RequestParam(required = false) MultipartFile inputFile,
                        @RequestParam(required = false) MultipartFile outputFile,
                        @RequestParam(required = false) MultipartFile illustration,
                        @RequestParam(required = false) String inputData,
                        @RequestParam(required = false) String outputData,
                        @RequestParam(defaultValue = "false") boolean isSample,
                        @RequestParam(defaultValue = "false") boolean isHidden,
                        @RequestParam(required = false) Integer orderIndex) throws IOException {

                com.kma.ojcore.dto.response.problems.TestCaseSdo saved = testCaseService.createTestCase(
                                problemId,
                                inputFile,
                                outputFile,
                                illustration,
                                inputData,
                                outputData,
                                isSample,
                                isHidden,
                                orderIndex);

                return ApiResponse.<com.kma.ojcore.dto.response.problems.TestCaseSdo>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Test case created successfully")
                                .data(saved)
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

        @PutMapping("/{testcaseId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<com.kma.ojcore.dto.response.problems.TestCaseSdo> updateTestCase(
                        @PathVariable UUID problemId,
                        @PathVariable UUID testcaseId,
                        @RequestBody UpdateTestCaseSdi request) {
                com.kma.ojcore.dto.response.problems.TestCaseSdo updated = testCaseService.updateTestCase(problemId,
                                testcaseId, request);

                return ApiResponse.<com.kma.ojcore.dto.response.problems.TestCaseSdo>builder()
                                .status(HttpStatus.OK.value())
                                .message("Test case updated successfully")
                                .data(updated)
                                .build();
        }

        @DeleteMapping("/{testcaseId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Void> deleteTestCase(
                        @PathVariable UUID problemId,
                        @PathVariable UUID testcaseId) {
                testCaseService.deleteTestCase(problemId, testcaseId);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("Test case deleted successfully")
                                .build();
        }

        @DeleteMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Void> deleteAllTestCases(
                        @PathVariable UUID problemId) {
                testCaseService.deleteAllTestCases(problemId);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .message("All test cases deleted successfully")
                                .build();
        }
}
