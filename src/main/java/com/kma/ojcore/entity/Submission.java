package com.kma.ojcore.entity;

import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "submissions", indexes = {
        @Index(name = "idx_submission_problem", columnList = "problem_id"), // Index cho cột problem_id để truy vấn nhanh các submission của một bài toán
        @Index(name = "idx_submission_user", columnList = "user_id")        // Index cho cột user_id để truy vấn nhanh các submission của một người dùng
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Submission extends BaseEntity {

    @Column(name = "language_key", nullable = false, length = 50)
    String languageKey; // Lưu "CPP", "JAVA", "PYTHON3"

    @Column(name = "source_code", columnDefinition = "TEXT", nullable = false)
    String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SubmissionStatus submissionStatus = SubmissionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    SubmissionVerdict verdict = SubmissionVerdict.PENDING;

    @Column(name = "retry_count")
    Integer retryCount = 0;

    // --- Kết quả đo lường ---
    @Column(name = "score")
    Integer score; // Điểm đạt được

    @Column(name = "passed_test_count")
    Integer passedTestCount; // Lưu số lượng testcase chạy đúng

    @Column(name = "total_test_count")
    Integer totalTestCount;  // Lưu tổng số testcase của bài

    @Column(name = "execution_time_ms")
    Long executionTimeMs; // Thời gian chạy dài nhất trong các testcase

    @Column(name = "execution_memory_mb")
    Long executionMemoryMb; // RAM tốn nhiều nhất (tùy chọn, sau này làm)

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage; // Lưu log báo lỗi nếu bị CE (Compile Error) hoặc RE

    // -- Relationships -- //

    // Submission - Problem //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    Problem problem;

    // Submission - User //
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}