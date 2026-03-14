package com.kma.ojcore.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.List;

public enum SubmissionVerdict {
    PENDING, // Chưa có phán quyết
    AC,      // Accepted (Đúng hoàn toàn)
    WA,      // Wrong Answer (Sai kết quả)
    TLE,     // Time Limit Exceeded (Quá thời gian)
    MLE,     // Memory Limit Exceeded (Tràn RAM)
    RE,      // Runtime Error (Lỗi thực thi, code crash)
    CE,      // Compile Error (Lỗi biên dịch)
    SE;       // System Error (Lỗi hệ thống máy chấm)

    @JsonCreator
    public static SubmissionVerdict fromString(String value) {
        if (value == null) return SE;
        try {
            return SubmissionVerdict.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SE; // Nếu máy chấm gửi bậy, tự ép về SE (System Error)
        }
    }

    public static List<SubmissionVerdict> getPublicVerdicts() {
        return Arrays.asList(AC, WA, TLE, MLE, RE, CE);
    }

    public static List<SubmissionVerdict> getAllVerdicts() {
        return Arrays.asList(values()); // Trả về toàn bộ các giá trị của Enum
    }
}