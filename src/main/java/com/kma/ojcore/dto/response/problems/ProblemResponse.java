package com.kma.ojcore.dto.response.problems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kma.ojcore.enums.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemResponse {

    UUID id;
    String title;
    String slug;
    ProblemDifficulty difficulty;
    EStatus status;
    ProblemStatus problemStatus;
    Long submissionCount;
    Long acceptedCount;
    Integer totalScore;
    RuleType ruleType;
    String userProblemState; // Trạng thái của người dùng hiện tại với bài toán này (Solved / Attempted)
    @JsonProperty("isAdded")
    Boolean isAdded; // Sẽ là true nếu bài này đã nằm trong contestId được gửi lên
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
