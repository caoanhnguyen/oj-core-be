package com.kma.ojcore.dto.response.users;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRankSdo {
    UUID id;
    String username;
    String avatarUrl;
    Integer acCount;
    Integer solvedCount;
    Integer submissionCount;
    Double totalScore;
    Integer rank;

    public UserRankSdo(UUID id, String username, String avatarUrl, Integer acCount, Integer solvedCount, Integer submissionCount, Double totalScore, Integer rank) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.acCount = acCount;
        this.solvedCount = solvedCount;
        this.submissionCount = submissionCount;
        this.totalScore = totalScore;
        this.rank = rank;
    }
}
