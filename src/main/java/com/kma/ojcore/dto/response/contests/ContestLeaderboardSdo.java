package com.kma.ojcore.dto.response.contests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ContestLeaderboardSdo {
    UUID userId;
    String username;
    Double score;
    Long penalty;
    // Map with Key = String (DisplayId) and Value = ContestProblemResultSdo
    java.util.Map<String, ContestProblemResultSdo> problemResults = new java.util.HashMap<>();

    // We can also have an additional constructor for legacy JPQL fetches
    public ContestLeaderboardSdo(UUID userId, String username, Double score, Long penalty) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.penalty = penalty;
        this.problemResults = new java.util.HashMap<>();
    }
}