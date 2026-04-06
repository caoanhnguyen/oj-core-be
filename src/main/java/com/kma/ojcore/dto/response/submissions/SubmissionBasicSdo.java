package com.kma.ojcore.dto.response.submissions;

import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionBasicSdo {
    UUID submissionId;
    EStatus status;

    // --- Kết quả đo lường ---
    SubmissionVerdict verdict;

    Double score;

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

    UUID contestId;

    String contestTitle;

    Double contestScore; // Điểm tính động trong contest

    // Constructor dùng riêng cho các HQL Query CŨ (Không có contestScore)
    public SubmissionBasicSdo(UUID submissionId, SubmissionVerdict verdict, Integer score,
                              Integer passedTestCount, Integer totalTestCount,
                              Long executionTimeMs, Long executionMemoryMb,
                              LocalDateTime createdDate, String languageKey,
                              UUID userId, String username,
                              UUID problemId, String problemTitle, String problemSlug,
                              EStatus status, UUID contestId, String contestTitle) {
        this.submissionId = submissionId;
        this.verdict = verdict;
        this.score = score != null ? score.doubleValue() : null;
        this.passedTestCount = passedTestCount;
        this.totalTestCount = totalTestCount;
        this.executionTimeMs = executionTimeMs;
        this.executionMemoryMb = executionMemoryMb;
        this.createdDate = createdDate;
        this.languageKey = languageKey;
        this.userId = userId;
        this.username = username;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.problemSlug = problemSlug;
        this.status = status;
        this.contestId = contestId;
        this.contestTitle = contestTitle;
    }

    // Constructor dùng cho các HQL Query có tính `contestScore`
    public SubmissionBasicSdo(UUID submissionId, SubmissionVerdict verdict, Integer rawScore,
                              Integer passedTestCount, Integer totalTestCount,
                              Long executionTimeMs, Long executionMemoryMb,
                              LocalDateTime createdDate, String languageKey,
                              UUID userId, String username,
                              UUID problemId, String problemTitle, String problemSlug,
                              Double contestScore, EStatus status, UUID contestId, String contestTitle) {
        this.submissionId = submissionId;
        this.verdict = verdict;
        this.score = contestScore != null ? contestScore : (rawScore != null ? rawScore.doubleValue() : null);
        this.passedTestCount = passedTestCount;
        this.totalTestCount = totalTestCount;
        this.executionTimeMs = executionTimeMs;
        this.executionMemoryMb = executionMemoryMb;
        this.createdDate = createdDate;
        this.languageKey = languageKey;
        this.userId = userId;
        this.username = username;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.problemSlug = problemSlug;
        this.contestScore = contestScore;
        this.status = status;
        this.contestId = contestId;
        this.contestTitle = contestTitle;
    }
}
