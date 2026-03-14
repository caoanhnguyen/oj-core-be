package com.kma.ojcore.dto.response.submissions;

import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionDetailsSdo {
    UUID submissionId;

    UUID userId;
    String username;

    UUID problemId;
    String problemTitle;

    String language;
    SubmissionStatus submissionStatus;
    SubmissionVerdict verdict;
    Integer score;

    // Chi tiết Testcase
    Integer passedTestCount;
    Integer totalTestCount;

    Long executionTimeMs;
    Long executionMemoryMb;
    LocalDateTime createdDate;

    // Dữ liệu nhạy cảm / Dung lượng lớn
    String errorMessage;
    String sourceCode;
}
