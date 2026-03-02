package com.kma.ojcore.service;

import com.kma.ojcore.entity.TestCase;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface TestCaseService {

    TestCase createTestCase(UUID problemId,
            MultipartFile inputFile,
            MultipartFile outputFile,
            MultipartFile illustration,
            String inputData,
            String outputData,
            boolean isSample,
            boolean isHidden,
            Integer orderIndex) throws IOException;

    void createTestCasesFromZip(UUID problemId, MultipartFile file, String metadata) throws IOException;
}
