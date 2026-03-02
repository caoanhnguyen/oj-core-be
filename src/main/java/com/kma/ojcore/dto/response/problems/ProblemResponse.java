package com.kma.ojcore.dto.response.problems;

import com.kma.ojcore.enums.ProblemDifficulty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemResponse {

    UUID id;
    String title;
    String slug;
    ProblemDifficulty difficulty;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
