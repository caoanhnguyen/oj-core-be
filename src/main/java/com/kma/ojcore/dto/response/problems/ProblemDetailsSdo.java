package com.kma.ojcore.dto.response.problems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kma.ojcore.enums.ProblemDifficulty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
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
    Integer timeLimitMs;
    Integer memoryLimitKb;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;

    List<ProblemTemplateSummary> templates;
    List<TestCaseSummary> testCases;
    List<ExampleSummary> examples;
    List<TopicsSummary> topics;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProblemTemplateSummary {
        UUID id;
        String language;
        String codeTemplate;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TestCaseSummary {
        UUID id;
        @JsonProperty("isHidden")
        boolean isSample;
        @JsonProperty("isSample")
        boolean isHidden;
        Integer orderIndex;
        String illustrationUrl;

        // Added for Display (Sample only)
        String inputData;
        String outputData;
        String inputUrl;
        String outputUrl;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExampleSummary {
        UUID id;
        String inputData;
        String outputData;
        String explanation; // HTML content
        Integer orderIndex;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopicsSummary {
        UUID topicId;
        String name;
    }
}
