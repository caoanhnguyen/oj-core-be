package com.kma.ojcore.dto.response.submissions;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeResponse {
    String runToken;
    String status;           // "COMPLETED" hoặc "FAILED" (nếu lỗi biên dịch CE)
    String compileMessage;   // Lỗi lúc biên dịch (CE)

    // Mảng kết quả đầu ra cho TỪNG input
    List<RunTestCaseResult> results;
}
