package com.kma.ojcore.dto.response.contests;

import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.RuleType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestBasicSdo {
    UUID id;
    String title;
    LocalDateTime startTime;
    LocalDateTime endTime;
    RuleType ruleType;
    ContestStatus contestStatus;
    ContestVisibility visibility;
    Long participantCount;
    EStatus status;
    Integer durationMinutes;

    public ContestBasicSdo(UUID id,
                           String title,
                           LocalDateTime startTime,
                           LocalDateTime endTime,
                           RuleType ruleType,
                           ContestVisibility visibility,
                           Long participantCount,
                           EStatus status,
                           Integer durationMinutes) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ruleType = ruleType;
        this.visibility = visibility;
        this.participantCount = participantCount;
        this.status = status;
        this.durationMinutes = durationMinutes;
    }
}