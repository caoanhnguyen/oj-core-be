package com.kma.ojcore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.repository.ProblemRepository;
import com.kma.ojcore.service.FileStorageService;
import com.kma.ojcore.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService minioService;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void processAndUploadTestcases(UUID problemId, MultipartFile zipFile) throws IOException {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Problem"));

        // 1. Tạo thư mục tạm trên Server BE để giải nén và làm việc
        Path tempDir = Files.createTempDirectory("oj_testcase_" + problemId);
        Map<String, byte[]> fileMap = new HashMap<>();

        try {
            // 2. Giải nén ZIP và lọc ra các file .in, .out
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    // Chỉ lấy file ở thư mục gốc hoặc xử lý path nếu cần
                    String fileName = Paths.get(entry.getName()).getFileName().toString();
                    if (fileName.endsWith(".in") || fileName.endsWith(".out")) {
                        fileMap.put(fileName, zis.readAllBytes());
                    }
                }
            }

            // 3. Phân tích các cặp Testcase (Tìm file .in phải có .out tương ứng)
            List<String> baseNames = fileMap.keySet().stream()
                    .filter(name -> name.endsWith(".in"))
                    .map(name -> name.replace(".in", ""))
                    .sorted() // Sắp xếp để chia điểm cho chuẩn
                    .toList();

            if (baseNames.isEmpty()) throw new RuntimeException("File ZIP không chứa testcase hợp lệ (.in/.out)");

            // 4. Logic chia điểm (Auto-split)
            int totalScore = (problem.getTotalScore() != null) ? problem.getTotalScore() : 100;
            int numTests = baseNames.size();
            int scorePerTest = totalScore / numTests;
            int extraScore = totalScore % numTests;

            // 5. Tính MD5, Size và Build info.json
            List<Map<String, Object>> testcaseList = new ArrayList<>();
            for (int i = 0; i < numTests; i++) {
                String base = baseNames.get(i);
                String inName = base + ".in";
                String outName = base + ".out";

                if (!fileMap.containsKey(outName)) {
                    throw new RuntimeException("Thiếu file kết quả tương ứng cho: " + inName);
                }

                Map<String, Object> tcInfo = new HashMap<>();
                tcInfo.put("inputName", inName);
                tcInfo.put("outputName", outName);
                tcInfo.put("inputSize", fileMap.get(inName).length);
                tcInfo.put("outputSize", fileMap.get(outName).length);
                tcInfo.put("inputMd5", DigestUtils.md5Hex(fileMap.get(inName)));
                tcInfo.put("outputMd5", DigestUtils.md5Hex(fileMap.get(outName)));

                // Chia điểm: dồn phần dư vào thằng cuối cùng
                tcInfo.put("score", (i == numTests - 1) ? (scorePerTest + extraScore) : scorePerTest);

                testcaseList.add(tcInfo);

                // Ghi file thô ra thư mục tạm để tí nữa upload cả folder lên MinIO
                Files.write(tempDir.resolve(inName), fileMap.get(inName));
                Files.write(tempDir.resolve(outName), fileMap.get(outName));
            }

            // 6. Ghi file info.json
            Map<String, Object> finalInfo = new HashMap<>();
            finalInfo.put("problemId", problemId);
            finalInfo.put("testCases", testcaseList);
            objectMapper.writeValue(tempDir.resolve("info.json").toFile(), finalInfo);

            // 7. Upload thư mục lên MinIO
            // Path trên MinIO ví dụ: problems/UUID/testcases/
            String minioPath = "problems/" + problemId + "/testcases";
            minioService.upload("oj-testcases",
                    minioPath + "/info.json",
                    Files.newInputStream(tempDir.resolve("info.json")),
                    Files.size(tempDir.resolve("info.json")),
                    "application/json");

            // 8. Cập nhật DB
            problem.setTestcaseDir(minioPath);
            problemRepository.save(problem);

            log.info("Xử lý thành công {} testcases cho bài toán {}", numTests, problemId);

        } finally {
            // 9. Luôn luôn dọn rác folder tạm
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}
