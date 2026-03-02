package com.kma.ojcore.service.impl;

import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.TestCase;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.exception.ResourceNotFoundException;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.repository.TestCaseRepository;
import com.kma.ojcore.service.FileStorageService;
import com.kma.ojcore.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final FileStorageService fileStorageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.paths.problem-root:problems}")
    private String problemRoot;

    @Value("${minio.paths.testcases-dir:testcases}")
    private String testcasesDir;

    @Value("${minio.paths.illustrations-dir:illustrations}")
    private String illustrationsDir;

    private String toSlug(String input) {
        if (input == null)
            return "unknown";
        String slug = input.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "unknown" : slug;
    }

    @Override
    @Transactional
    public TestCase createTestCase(UUID problemId,
            MultipartFile inputFile,
            MultipartFile outputFile,
            MultipartFile illustration,
            String inputData,
            String outputData,
            boolean isSample,
            boolean isHidden,
            Integer orderIndex) throws IOException {

        Problem problem = problemRepository.findByIdAndStatus(problemId, EStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + problemId));

        String problemFolder = toSlug(problem.getTitle()) + "-" + problem.getId();

        TestCase testCase = TestCase.builder()
                .problem(problem)
                .isSample(isSample)
                .isHidden(isHidden)
                .orderIndex(orderIndex)
                .build();

        // Input
        if (inputFile != null && !inputFile.isEmpty()) {
            // 1. Upload to MinIO (Always)
            String originalName = inputFile.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String objectName = String.format("%s/%s/%s/input-%s%s",
                    problemRoot, problemFolder, testcasesDir, UUID.randomUUID(), extension);
            String stored = fileStorageService.upload(bucketName, objectName,
                    inputFile.getInputStream(), inputFile.getSize(), inputFile.getContentType());
            testCase.setInputUrl(stored);

            // 2. Optimization: If small (< 10KB), store content in DB for quick display
            if (inputFile.getSize() < 10240) { // 10KB
                try {
                    // Read content as String (Assuming text files)
                    String content = new String(inputFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    testCase.setInputData(content);
                } catch (Exception e) {
                    // Ignore encoding errors, just skip DB storage
                }
            }
        } else {
            testCase.setInputData(inputData);
        }

        // Output
        if (outputFile != null && !outputFile.isEmpty()) {
            // 1. Upload to MinIO (Always)
            String originalName = outputFile.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String objectName = String.format("%s/%s/%s/output-%s%s",
                    problemRoot, problemFolder, testcasesDir, UUID.randomUUID(), extension);
            String stored = fileStorageService.upload(bucketName, objectName,
                    outputFile.getInputStream(), outputFile.getSize(), outputFile.getContentType());
            testCase.setOutputUrl(stored);

            // 2. Optimization: If small (< 10KB), store content in DB
            if (outputFile.getSize() < 10240) { // 10KB
                try {
                    String content = new String(outputFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    testCase.setOutputData(content);
                } catch (Exception e) {
                    // Ignore
                }
            }
        } else {
            testCase.setOutputData(outputData);
        }

        // Illustration
        if (illustration != null && !illustration.isEmpty()) {
            String originalName = illustration.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.'));
            }
            String objectName = String.format("%s/%s/%s/illustration-%s%s",
                    problemRoot, problemFolder, illustrationsDir, UUID.randomUUID(), extension);
            String stored = fileStorageService.upload(bucketName, objectName,
                    illustration.getInputStream(), illustration.getSize(), illustration.getContentType());
            testCase.setIllustrationUrl(stored);
        }

        return testCaseRepository.save(testCase);
    }

    @Override
    @Transactional
    public void createTestCasesFromZip(UUID problemId, MultipartFile file, String metadata) throws IOException {
        // 1. Parse Metadata
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<java.util.Map<String, Object>> testcases = mapper.readValue(
                metadata,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {
                });

        // 2. Extract Zip to Map
        java.util.Map<String, byte[]> fileMap = new java.util.HashMap<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(file.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // Normalize name: "1.in" vs "folder/1.in" -> take filename only?
                    // Frontend puts them in root.
                    String name = new java.io.File(entry.getName()).getName();
                    fileMap.put(name, zis.readAllBytes());
                }
            }
        }

        // 3. Process each testcase from metadata
        for (int i = 0; i < testcases.size(); i++) {
            java.util.Map<String, Object> tc = testcases.get(i);
            String name = (String) tc.get("name"); // e.g., "1"
            String inputName = name + ".in";
            String outputName = name + ".out";

            // Check if files exist
            if (fileMap.containsKey(inputName) && fileMap.containsKey(outputName)) {
                boolean isHidden = Boolean.TRUE.equals(tc.get("isHidden"));
                boolean isSample = !isHidden; // Derived from FE Toggle

                // Create custom MultipartFile
                MultipartFile inFile = new ByteArrayMultipartFile(fileMap.get(inputName), inputName, "text/plain");
                MultipartFile outFile = new ByteArrayMultipartFile(fileMap.get(outputName), outputName, "text/plain");

                createTestCase(
                        problemId,
                        inFile,
                        outFile,
                        null, // illustration
                        null, // inputData
                        null, // outputData
                        isSample,
                        isHidden,
                        i + 1 // orderIndex
                );
            }
        }
    }

    // Helper Class for Byte Array to MultipartFile
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
