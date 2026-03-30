package com.kma.ojcore.dto.response.contests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ContestLeaderboardSdo {
    UUID userId;
    String username;
    Double score;
    Long penalty;
}