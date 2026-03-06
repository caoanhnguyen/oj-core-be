package com.kma.ojcore.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface TestcaseService {
    void processAndUploadTestcases(UUID problemId, MultipartFile zipFile) throws IOException;
}
