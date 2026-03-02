package com.kma.ojcore.dto.request.problems;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO cho việc tạo/cập nhật Problem Example
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExampleSdi {

    @NotBlank(message = "Input data is required")
    String inputData;

    @NotBlank(message = "Output data is required")
    String outputData;

    /**
     * HTML content giải thích example
     * Có thể chứa ảnh minh họa
     */
    String explanation;

    @NotNull(message = "Order index is required")
    Integer orderIndex;
}
