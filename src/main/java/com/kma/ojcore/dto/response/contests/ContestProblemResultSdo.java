package com.kma.ojcore.dto.response.contests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ContestProblemResultSdo {
    UUID problemId;
    String displayId; // A, B, C...
    Double score;
    Long penalty;     // in minutes
    Integer tries;    // number of attempts (used in ACM usually)
    Boolean isAc;     // true if AC
}
