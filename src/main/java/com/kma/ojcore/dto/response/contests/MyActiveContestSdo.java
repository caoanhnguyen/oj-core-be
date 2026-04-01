package com.kma.ojcore.dto.response.contests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyActiveContestSdo {
    ContestBasicSdo contest;
    LocalDateTime sessionEndTime;

    public MyActiveContestSdo(ContestBasicSdo contest, LocalDateTime sessionEndTime) {
        this.contest = contest;
        this.sessionEndTime = sessionEndTime;
    }
}
