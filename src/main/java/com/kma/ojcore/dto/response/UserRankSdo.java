package com.kma.ojcore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRankSdo {
    UUID id;
    String username;
    String avatarUrl;
    Integer acCount;
    Integer solvedCount;
    Integer submissionCount;
    Double totalScore;
}
