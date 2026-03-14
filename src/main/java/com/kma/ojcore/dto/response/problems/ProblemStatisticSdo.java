package com.kma.ojcore.dto.response.problems;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ProblemStatisticSdo {
    long totalSubmissions;
    Map<String, Long> verdictCounts; // Ví dụ: {"AC": 4665, "WA": 946, "CE": 2058}
}
