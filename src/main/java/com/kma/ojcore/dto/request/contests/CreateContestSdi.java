package com.kma.ojcore.dto.request.contests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.RuleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateContestSdi {

    @NotBlank(message = "Title is required.")
    String title;

    @NotBlank(message = "Description is required.")
    String description;

    @NotNull(message = "Start time is required.")
    @Future(message = "Start time must be in the future.")
    LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    LocalDateTime endTime;

    @NotNull(message = "Rule type is required.")
    RuleType ruleType;

    @NotNull(message = "Visibility is required.")
    ContestVisibility visibility;

    String password;

    // 1. CHECK THỜI GIAN (End phải lớn hơn Start)
    @JsonIgnore
    @AssertTrue(message = "Start time must before end time.")
    public boolean isTimeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    // 2. CHECK PASSWORD (Nếu PRIVATE thì phải có Pass)
    @JsonIgnore
    @AssertTrue(message = "Private contests must have password.")
    public boolean isPasswordValid() {
        if (visibility == ContestVisibility.PRIVATE) {
            return password != null && !password.trim().isEmpty();
        }
        return true;
    }
}