package com.kma.ojcore.enums;

import lombok.Getter;

@Getter
public enum SupportedLanguage {
    // 1. Nhóm Native (Biên dịch trực tiếp ra mã máy) -> Làm hệ quy chiếu gốc (x1)
    C("C", 1.0, 1.0),
    CPP("C++", 1.0, 1.0),

    // 2. Nhóm máy ảo (JVM) -> Khởi động chậm, ngốn RAM nền
    // Cấp gấp đôi thời gian (x2) và gấp đôi RAM (x2)
    JAVA("Java", 2.0, 2.0),

    // 3. Nhóm thông dịch (Interpreted) -> Chạy thuật toán vòng lặp rất chậm
    // Python cần thời gian chạy rất dài, RAM xài cũng khá tốn
    PYTHON3("Python 3", 3.0, 1.5);

    private final String displayName;
    private final double timeMultiplier; // Hệ số nhân thời gian chạy so với nhóm Native (C/C++)
    private final double memoryMultiplier; // Hệ số nhân RAM so với nhóm Native (C/C++)

    SupportedLanguage(String displayName, double timeMultiplier, double memoryMultiplier) {
        this.displayName = displayName;
        this.timeMultiplier = timeMultiplier;
        this.memoryMultiplier = memoryMultiplier;
    }
}