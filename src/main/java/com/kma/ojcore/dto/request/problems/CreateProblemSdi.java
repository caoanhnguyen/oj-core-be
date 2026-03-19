package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.RuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProblemSdi {
    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    @NotBlank(message = "Slug không được để trống")
    String slug;

    @NotBlank(message = "Mô tả bài toán không được để trống")
    String description;

    String constraints;

    @NotNull(message = "Phải chọn độ khó")
    ProblemDifficulty difficulty;

    @NotNull(message = "Phải cài đặt giới hạn thời gian")
    Integer timeLimitMs;

    @NotNull(message = "Phải cài đặt giới hạn bộ nhớ")
    Integer memoryLimitMb;

    @NotNull(message = "Phải chọn luật chấm (ACM/OI)")
    RuleType ruleType;

    // Optional: Tổng điểm (Nếu FE không gửi, BE tự set = 100)
    Integer totalScore;

    // Testcase Dir có thể null lúc mới tạo (FE sẽ gọi API upload file ZIP sau để cập nhật)
    String testcaseDir;

    @NotEmpty(message = "Phải cho phép ít nhất 1 ngôn ngữ")
    Set<String> allowedLanguages = new HashSet<>();

    String source;

    String hint;

    String inputFormat;

    String outputFormat;

    /**
     * Danh sách examples cho Problem
     */
    @Valid
    List<ExampleSdi> examples;

    /**
     * Danh sách object keys của ảnh temporary đã upload
     */
    List<String> temporaryImageKeys;

    /**
     * Template code cho từng ngôn ngữ
     */
    @Valid
    List<TemplateSdi> templates;

    Set<UUID> topicIds;
}