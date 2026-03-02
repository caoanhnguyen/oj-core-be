package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.ProblemDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProblemSdi {
    @NotBlank
    String title;

    @NotBlank
    String slug;

    @NotBlank
    String description;

    @NotBlank
    String constraints;

    @NotNull
    ProblemDifficulty difficulty;

    @NotNull
    Integer timeLimitMs;

    @NotNull
    Integer memoryLimitKb;

    /**
     * Danh sách examples cho Problem
     */
    List<ExampleSdi> examples;

    /**
     * Danh sách object keys của ảnh temporary đã upload
     * Backend sẽ commit những ảnh này khi tạo Problem
     * VD: ["temp/abc-123.png", "temp/def-456.jpg"]
     */
    List<String> temporaryImageKeys;

    List<TemplateSdi> templates;
}
