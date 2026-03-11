package com.kma.ojcore.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SubmissionStatus {
    PENDING,    // Vừa nộp, đang chờ trong hàng đợi RabbitMQ
    JUDGING,    // Máy chấm đã nhận và đang chạy code
    COMPLETED,  // Đã chấm xong (có kết quả AC, WA, TLE...)
    FAILED;      // Lỗi hệ thống (VD: Không tải được testcase, RabbitMQ sập)

    @JsonCreator
    public static SubmissionStatus fromString(String value) {
        if (value == null) return FAILED;
        try {
            return SubmissionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FAILED; // Nếu máy chấm gửi bậy, tự ép về FAILED
        }
    }
}