package com.kma.ojcore.dto.request.problems;

import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.enums.SupportedLanguage;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateProblemSdi {

    String title;

    String slug;

    String description;

    String constraints;

    ProblemDifficulty difficulty;

    Integer timeLimitMs;

    Integer memoryLimitKb;

    RuleType ruleType;

    Integer totalScore;

    String source;

    String hint;

    String inputFormat;

    String outputFormat;

    String testcaseDir;

    Set<SupportedLanguage> allowedLanguages;

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

    Set<UUID> topicIds;
}
