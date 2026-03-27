package com.kma.ojcore.dto.request.contests;

import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.RuleType;
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

    @NotBlank
    String title;

    @NotBlank
    String description;

    @NotNull
    LocalDateTime startTime;

    @NotNull
    LocalDateTime endTime;

    @NotNull
    RuleType ruleType;

    @NotNull
    ContestVisibility visibility;

    String password;
}