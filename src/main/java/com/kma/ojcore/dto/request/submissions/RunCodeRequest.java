package com.kma.ojcore.dto.response.submissions;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RunCodeRequest {
    String runToken;       // Mã định danh phiên chạy (UUID)
    String sourceCode;     // Mã nguồn sinh viên viết
    String sourceName;     // VD: "Main.java", "main.cpp"
    String languageKey;    // VD: "JAVA", "CPP"

    // Danh sách các Custom Input do user nhập
    List<String> customInputs;

    // Các lệnh cấu hình (Core BE lấy từ DB gửi sang)
    boolean isCompiled;
    String compileCommand;
    String runCommand;

    // Giới hạn tài nguyên (Có thể lấy mặc định: 2s, 256MB)
    Long timeLimitMs;
    Long memoryLimitMb;
}
