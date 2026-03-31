package com.kma.ojcore.dto.response.contests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestParticipationSdo {
    UUID userId;
    String username;
    String email;
    @JsonProperty("isDisqualified")
    boolean isDisqualified;
    LocalDateTime startTime;
    LocalDateTime endTime;
    @JsonProperty("isFinished")
    boolean isFinished;
    Double score;
    Long penalty;
}