package com.kma.ojcore.dto.request.submissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionSdi {

    @NotNull(message = "Problem ID không được để trống")
    UUID problemId;

    @NotBlank(message = "Ngôn ngữ không được để trống (VD: CPP, JAVA)")
    String languageKey;

    @NotBlank(message = "Source code không được để trống")
    String sourceCode;

    UUID contestId;
}
