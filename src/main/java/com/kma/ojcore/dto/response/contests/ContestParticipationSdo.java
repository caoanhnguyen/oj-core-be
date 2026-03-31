package com.kma.ojcore.dto.response.contests;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestParticipationSdo {
    UUID userId;
    String username;
    String email;
    boolean isDisqualified;
    java.time.LocalDateTime startTime;
    java.time.LocalDateTime endTime;
    boolean isFinished;
    Double score;
    Long penalty;
}