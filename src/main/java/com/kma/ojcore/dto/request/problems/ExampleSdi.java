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

    // Đổi thành rawInput cho chuẩn với Entity
    @NotBlank(message = "Input thô là bắt buộc cho Example")
    String rawInput;

    // Đổi thành rawOutput cho chuẩn với Entity
    @NotBlank(message = "Output thô là bắt buộc cho Example")
    String rawOutput;

    /**
     * HTML content giải thích example
     * Trường này có thể để trống nếu không cần giải thích
     */
    String explanation;

    @NotNull(message = "Thứ tự hiển thị (Order index) là bắt buộc")
    Integer orderIndex;
}