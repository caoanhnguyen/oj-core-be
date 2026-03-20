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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Problem"));

        // 1. Tạo thư mục tạm trên Server BE để lưu file info.json
        Path tempDir = Files.createTempDirectory("oj_testcase_" + problemId);
        Map<String, byte[]> fileMap = new HashMap<>();

        try {
            // 2. Đọc file ZIP trực tiếp trên RAM, lọc ra các file .in, .out
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    // Lấy tên file gốc (bỏ qua các thư mục lồng nhau nếu có)
                    String fileName = Paths.get(entry.getName()).getFileName().toString();

                    // Loại bỏ các file rác ẩn của MacOS
                    if (!fileName.startsWith("._") && !fileName.equals(".DS_Store") && !entry.getName().contains("__MACOSX")) {
                        if (fileName.endsWith(".in") || fileName.endsWith(".out")) {
                            fileMap.put(fileName, zis.readAllBytes());
                        }
                    }
                }
            }

            // 3. Phân tích các cặp Testcase (Tìm file .in phải có .out tương ứng)
            List<String> baseNames = fileMap.keySet().stream()
                    .filter(name -> name.endsWith(".in"))
                    .map(name -> name.replace(".in", ""))
                    .sorted() // Sắp xếp để chia điểm (1, 2, 3...)
                    .toList();

            if (baseNames.isEmpty()) throw new RuntimeException("File ZIP không chứa testcase hợp lệ (.in/.out)");

            // 4. Logic chia điểm (Auto-split)
            int totalScore = (problem.getTotalScore() != null) ? problem.getTotalScore() : 100;
            int numTests = baseNames.size();
            int scorePerTest = totalScore / numTests;
            int extraScore = totalScore % numTests;

            // 5. Tính MD5, Size và Build cấu trúc info.json
            List<Map<String, Object>> testcaseList = new ArrayList<>();
            for (int i = 0; i < numTests; i++) {
                String base = baseNames.get(i);
                String inName = base + ".in";
                String outName = base + ".out";

                if (!fileMap.containsKey(outName)) {
                    throw new RuntimeException("Thiếu file kết quả tương ứng cho: " + inName);
                }

                byte[] inBytes = fileMap.get(inName);
                byte[] outBytes = fileMap.get(outName);

                // LÀM SẠCH OUTPUT TRƯỚC KHI BĂM MD5
                String rawOutputStr = new String(outBytes, StandardCharsets.UTF_8);
                String strippedOutputStr = rawOutputStr.replaceAll("(?m)[ \\t]+$", "").replace("\r\n", "\n").trim();
                String strippedOutputMd5 = DigestUtils.md5Hex(strippedOutputStr.getBytes(StandardCharsets.UTF_8));

                Map<String, Object> tcInfo = new HashMap<>();
                tcInfo.put("inputName", inName);
                tcInfo.put("outputName", outName);
                tcInfo.put("inputSize", inBytes.length);
                tcInfo.put("outputSize", outBytes.length);

                // Lưu lại MD5 đã được làm sạch để máy chấm lấy ra so sánh trực tiếp
                tcInfo.put("strippedOutputMd5", strippedOutputMd5);

                // Chia điểm: dồn phần dư vào thằng cuối cùng
                tcInfo.put("score", (i == numTests - 1) ? (scorePerTest + extraScore) : scorePerTest);

                testcaseList.add(tcInfo);
            }

            // 6. Ghi file info.json ra ổ cứng tạm
            Map<String, Object> finalInfo = new HashMap<>();
            finalInfo.put("problemId", problemId.toString());
            finalInfo.put("testCases", testcaseList);

            Path infoJsonPath = tempDir.resolve("info.json");
            objectMapper.writeValue(infoJsonPath.toFile(), finalInfo);

            // 7. Upload lên MinIO
            String minioPath = String.format("%s/%s/%s", problemPrefix, problemId, testcaseSuffix);

            // 7.1. Upload info.json
            minioService.upload(
                    testcaseBucket,
                    minioPath + "/info.json",
                    Files.newInputStream(infoJsonPath),
                    Files.size(infoJsonPath),
                    "application/json"
            );

            // 7.2. Upload testcases.zip
            minioService.upload(
                    testcaseBucket,
                    minioPath + "/testcases.zip",
                    zipFile.getInputStream(),
                    zipFile.getSize(),
                    "application/zip"
            );

            // 8. Cập nhật đường dẫn vào DB
            problem.setTestcaseDir(minioPath);
            problemRepository.save(problem);

            log.info("Xử lý và upload thành công {} testcases cho bài toán {}", numTests, problemId);

        } finally {
            // 9. Xóa thư mục tạm sau khi hoàn thành
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}