package com.kma.ojcore.dto.response.submissions;

import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionDetailsSdo {
    UUID submissionId;

    UUID userId;
    String username;

    UUID problemId;
    String problemTitle;
    String problemSlug;

    String language;
    SubmissionStatus submissionStatus;
    SubmissionVerdict verdict;
    Double score;

    // Chi tiết Testcase
    Integer passedTestCount;
    Integer totalTestCount;

    Long executionTimeMs;
    Long executionMemoryMb;
    LocalDateTime createdDate;

    // Dữ liệu nhạy cảm / Dung lượng lớn
    String errorMessage;
    String sourceCode;

    Double contestScore;

    // Constructor cho HQL cũ, không có contestScore
    public SubmissionDetailsSdo(UUID submissionId, UUID userId, String username,
                                UUID problemId, String problemTitle, String problemSlug,
                                String language, SubmissionStatus submissionStatus,
                                SubmissionVerdict verdict, Integer rawScore,
                                Integer passedTestCount, Integer totalTestCount,
                                Long executionTimeMs, Long executionMemoryMb,
                                LocalDateTime createdDate, String errorMessage,
                                String sourceCode) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.username = username;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.problemSlug = problemSlug;
        this.language = language;
        this.submissionStatus = submissionStatus;
        this.verdict = verdict;
        this.score = rawScore != null ? rawScore.doubleValue() : null;
        this.passedTestCount = passedTestCount;
        this.totalTestCount = totalTestCount;
        this.executionTimeMs = executionTimeMs;
        this.executionMemoryMb = executionMemoryMb;
        this.createdDate = createdDate;
        this.errorMessage = errorMessage;
        this.sourceCode = sourceCode;
    }

    // Constructor có contestScore, ghi đè score hiển thị
    public SubmissionDetailsSdo(UUID submissionId, UUID userId, String username,
                                UUID problemId, String problemTitle, String problemSlug,
                                String language, SubmissionStatus submissionStatus,
                                SubmissionVerdict verdict, Integer rawScore,
                                Integer passedTestCount, Integer totalTestCount,
                                Long executionTimeMs, Long executionMemoryMb,
                                LocalDateTime createdDate, String errorMessage,
                                String sourceCode, Double contestScore) {
        this(submissionId, userId, username, problemId, problemTitle, problemSlug,
             language, submissionStatus, verdict, rawScore, passedTestCount, totalTestCount,
             executionTimeMs, executionMemoryMb, createdDate, errorMessage, sourceCode);
        this.score = contestScore != null ? contestScore : (rawScore != null ? rawScore.doubleValue() : null);
        this.contestScore = contestScore;
    }
}
