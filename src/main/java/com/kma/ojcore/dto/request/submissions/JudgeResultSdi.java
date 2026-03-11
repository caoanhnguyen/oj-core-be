package com.kma.ojcore.dto.request.submissions;

import com.kma.ojcore.enums.SubmissionStatus;
import com.kma.ojcore.enums.SubmissionVerdict;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JudgeResultSdi {

    // ID của bài nộp để biết đường mà update
    UUID submissionId;

    // Trạng thái (Thường trả về COMPLETED hoặc FAILED)
    SubmissionStatus submissionStatus;

    // Phán quyết cuối cùng (AC, WA, TLE, RE...)
    SubmissionVerdict submissionVerdict;

    // Các chỉ số đo lường
    Integer score;
    Integer passedTestCount;
    Integer totalTestCount;
    Long executionTimeMs;
    Long executionMemoryMb;

    // Báo lỗi (Nếu bị CE - Compile Error hoặc RE - Runtime Error)
    String errorMessage;
}