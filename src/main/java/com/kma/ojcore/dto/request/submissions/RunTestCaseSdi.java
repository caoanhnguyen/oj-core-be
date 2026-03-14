package com.kma.ojcore.dto.request.submissions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RunTestCaseSdi {

    String input;
    String expectedOutput; // Mang giá trị null nếu là custom input, không có expected output
}
