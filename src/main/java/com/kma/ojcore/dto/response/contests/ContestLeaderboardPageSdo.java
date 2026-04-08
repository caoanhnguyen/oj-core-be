package com.kma.ojcore.dto.response.contests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestLeaderboardPageSdo {
    List<ContestLeaderboardSdo> content;
    long totalElements;
    int totalPages;
    int size;
    int number;
    List<ContestProblemSdo> problems;
}
