package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.service.FileStorageService;
import com.kma.ojcore.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestcasesServiceImpl implements TestcaseService {

    @Value("${oj.storage.minio.bucket-testcase}")
    private String testcaseBucket;

    @Value("${oj.storage.minio.prefix-problem}")
    private String problemPrefix;

    @Value("${oj.storage.minio.suffix-testcase}")
    private String testcaseSuffix;

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService minioService;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void processAndUploadTestcases(UUID problemId, MultipartFile zipFile) throws IOException {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        Path tempDir = Files.createTempDirectory("oj_testcase_" + problemId);
        Map<String, byte[]> fileMap = new HashMap<>();
        String configJsonStr = null;

        try {
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    String fileName = Paths.get(entry.getName()).getFileName().toString();

                    if (!fileName.startsWith("._") && !fileName.equals(".DS_Store") && !entry.getName().contains("__MACOSX")) {
                        if (fileName.equals("config.json")) {
                            configJsonStr = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                        } else if (fileName.endsWith(".in") || fileName.endsWith(".out")) {
                            fileMap.put(fileName, zis.readAllBytes());
                        }
                    }
                }
            }

            List<String> baseNames = fileMap.keySet().stream()
                    .filter(name -> name.endsWith(".in"))
                    .map(name -> name.replace(".in", ""))
                    .sorted()
                    .toList();

            if (baseNames.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_TESTCASE_ARCHIVE);
            }

            // Output data placeholders
            List<Map<String, Object>> subtaskList = new ArrayList<>();
            List<Map<String, Object>> plainTestcaseList = new ArrayList<>();

            int totalScore = (problem.getTotalScore() != null) ? problem.getTotalScore() : 100;

            if (configJsonStr != null) {
                // =============== CÓ CẤU HÌNH SUBTASKS (Subtask Mode) ===============
                log.info("Detected config.json for problem {}. Parsing subtasks...", problemId);
                Map<String, Object> configData;
                try {
                    configData = objectMapper.readValue(configJsonStr, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.error("Malformed config.json for problem {}: {}", problemId, e.getMessage());
                    throw new BusinessException(ErrorCode.INVALID_TESTCASE_ARCHIVE,
                            "config.json có cú pháp JSON không hợp lệ. Vui lòng kiểm tra lại tệp config.json.");
                }
                List<Map<String, Object>> configSubtasks = (List<Map<String, Object>>) configData.get("subtasks");

                if (configSubtasks != null) {
                    for (Map<String, Object> cSub : configSubtasks) {
                        Map<String, Object> subData = new HashMap<>();
                        subData.put("subtaskId", cSub.get("id"));
                        subData.put("score", cSub.get("score"));

                        List<String> tcNames = (List<String>) cSub.get("testcases");
                        List<Map<String, Object>> processedTcs = new ArrayList<>();

                        for (String tc : tcNames) {
                            String base = tc.replace(".in", "");
                            String inName = base + ".in";
                            String outName = base + ".out";

                            if (!fileMap.containsKey(outName) || !fileMap.containsKey(inName)) {
                                throw new BusinessException(ErrorCode.MISSING_OUTPUT_FILE,
                                        "config.json tham chiếu đến testcase '" + base + "' nhưng không tìm thấy cặp tệp tương ứng trong file ZIP.");
                            }
                            processedTcs.add(buildTestCaseInfoMap(inName, outName, fileMap, 0)); // Điểm lẻ sẽ ghi vào subtask, testcase để score=0
                        }
                        subData.put("testCases", processedTcs);
                        subtaskList.add(subData);
                    }
                }
            } else {
                // =============== KHÔNG CÓ CẤU HÌNH (Legacy / Standard Mode) ===============
                log.info("No config.json found for problem {}. Using basic equitable division...", problemId);
                int numTests = baseNames.size();
                int scorePerTest = totalScore / numTests;
                int extraScore = totalScore % numTests;

                for (int i = 0; i < numTests; i++) {
                    String base = baseNames.get(i);
                    String inName = base + ".in";
                    String outName = base + ".out";

                    if (!fileMap.containsKey(outName)) {
                        throw new BusinessException(ErrorCode.MISSING_OUTPUT_FILE, "Missing output file for: " + inName);
                    }
                    int currentScore = (i == numTests - 1) ? (scorePerTest + extraScore) : scorePerTest;
                    plainTestcaseList.add(buildTestCaseInfoMap(inName, outName, fileMap, currentScore));
                }
            }

            // Ghi ra info.json
            Map<String, Object> finalInfo = new HashMap<>();
            finalInfo.put("problemId", problemId.toString());
            if (!subtaskList.isEmpty()) {
                finalInfo.put("subtasks", subtaskList);
            }
            if (!plainTestcaseList.isEmpty()) {
                finalInfo.put("testCases", plainTestcaseList);
            }

            Path infoJsonPath = tempDir.resolve("info.json");
            objectMapper.writeValue(infoJsonPath.toFile(), finalInfo);

            String minioPath = String.format("%s/%s/%s", problemPrefix, problemId, testcaseSuffix);

            minioService.upload(
                    testcaseBucket,
                    minioPath + "/info.json",
                    Files.newInputStream(infoJsonPath),
                    Files.size(infoJsonPath),
                    "application/json"
            );

            minioService.upload(
                    testcaseBucket,
                    minioPath + "/testcases.zip",
                    zipFile.getInputStream(),
                    zipFile.getSize(),
                    "application/zip"
            );

            problem.setTestcaseDir(minioPath);
            problemRepository.save(problem);

            log.info("Successfully processed and uploaded testcases for problem {}", problemId);

        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    private Map<String, Object> buildTestCaseInfoMap(String inName, String outName, Map<String, byte[]> fileMap, int score) {
        byte[] inBytes = fileMap.get(inName);
        byte[] outBytes = fileMap.get(outName);

        String rawOutputStr = new String(outBytes, StandardCharsets.UTF_8);
        String strippedOutputStr = rawOutputStr.replaceAll("(?m)[ \\t]+$", "").replace("\r\n", "\n").trim();
        String strippedOutputMd5 = DigestUtils.md5Hex(strippedOutputStr.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> tcInfo = new HashMap<>();
        tcInfo.put("inputName", inName);
        tcInfo.put("outputName", outName);
        tcInfo.put("inputSize", inBytes.length);
        tcInfo.put("outputSize", outBytes.length);
        tcInfo.put("strippedOutputMd5", strippedOutputMd5);
        tcInfo.put("score", score);
        return tcInfo;
    }
}