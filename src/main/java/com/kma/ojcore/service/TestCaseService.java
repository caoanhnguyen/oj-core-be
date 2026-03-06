package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.problems.UpdateTestCaseSdi;
import com.kma.ojcore.dto.response.problems.TestCaseSdo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface TestCaseService {

    TestCaseSdo createTestCase(UUID problemId,
            MultipartFile inputFile,
            MultipartFile outputFile,
            MultipartFile illustration,
            String inputData,
            String outputData,
            boolean isSample,
            boolean isHidden,
            Integer orderIndex) throws IOException;

    void createTestCasesFromZip(UUID problemId, MultipartFile file, String metadata) throws IOException;

    TestCaseSdo updateTestCase(UUID problemId, UUID testcaseId, UpdateTestCaseSdi request);

    void deleteTestCase(UUID problemId, UUID testcaseId);

    void deleteAllTestCases(UUID problemId);
}
