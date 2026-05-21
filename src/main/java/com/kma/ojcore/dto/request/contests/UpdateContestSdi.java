package com.kma.ojcore.dto.request.contests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kma.ojcore.enums.ContestFormat;
import com.kma.ojcore.enums.ContestResourceVisibility;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.enums.ScoreboardVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateContestSdi {

    @NotBlank(message = "Title is required.")
    String title;

    @NotBlank(message = "Contest key is required.")
    String contestKey;

    @NotBlank(message = "Description is required.")
    String description;

    @NotNull(message = "Start time is required.")
    LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    LocalDateTime endTime;

    @NotNull(message = "Rule type is required.")
    RuleType ruleType;

    @NotNull(message = "Visibility is required.")
    ContestVisibility visibility;

    String password;

    @NotNull(message = "Format is required.")
    ContestFormat format;

    Integer durationMinutes;
    Boolean allowLateRegistration;

    @NotNull(message = "Scoreboard visibility is required.")
    ScoreboardVisibility scoreboardVisibility;

    @NotNull(message = "Resource visibility is required.")
    ContestResourceVisibility resourceVisibility;

    @JsonIgnore
    @AssertTrue(message = "Start time must before end time.")
    public boolean isTimeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}
