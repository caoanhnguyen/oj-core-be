package com.kma.ojcore.dto.response.submissions;

import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionBasicSdo {
    UUID submissionId;

    // --- Kết quả đo lường ---
    SubmissionVerdict verdict;

    Integer score;

    Integer passedTestCount;

    Integer totalTestCount;

    Long executionTimeMs;

    Long executionMemoryMb;

    LocalDateTime createdDate;

    String languageKey;


    // --- Thông tin tác giả ---
    UUID userId;

    String username;

    // -- Thông tin problem
    UUID problemId;

    String problemTitle;

    String problemSlug;
}
