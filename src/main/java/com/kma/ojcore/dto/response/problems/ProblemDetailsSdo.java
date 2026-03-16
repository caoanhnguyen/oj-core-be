package com.kma.ojcore.dto.response.problems;

import com.kma.ojcore.enums.ProblemDifficulty;
import com.kma.ojcore.enums.RuleType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemDetailsSdo {
    UUID id;
    String title;
    String slug;
    String description;
    String constraints;
    ProblemDifficulty difficulty;

    // Giới hạn chạy
    Integer timeLimitMs;
    Integer memoryLimitMb;
    RuleType ruleType;

    // Thống kê & UI
    Long submissionCount;
    Long acceptedCount;
    Integer totalScore;
    String source;
    String hint;
    String authorName;
    String inputFormat;
    String outputFormat;

    // Cấu hình ngôn ngữ
    Set<String> allowedLanguages;

    LocalDateTime createdDate;
    LocalDateTime updatedDate;

    // Các danh sách con
    List<ProblemTemplateSummary> templates;
    List<ExampleSummary> examples;
    List<TopicsSummary> topics;

    // ĐÃ XÓA TestCaseSummary VÌ LƯU Ở MINIO RỒI

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProblemTemplateSummary {
        UUID id;
        String languageKey;
        String codeTemplate;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExampleSummary {
        UUID id;
        String rawInput;   // Đổi tên cho chuẩn Entity
        String rawOutput;  // Đổi tên cho chuẩn Entity
        String explanation;
        Integer orderIndex;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopicsSummary {
        UUID topicId; // Map từ id của Topic
        String name;
        String slug;
    }
}