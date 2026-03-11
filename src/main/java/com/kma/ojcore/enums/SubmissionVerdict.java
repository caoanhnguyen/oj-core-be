package com.kma.ojcore.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

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
}