package com.kma.ojcore.dto.response.contests;

import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
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
public class ContestDetailSdo {
    UUID id;
    String title;
    String description;
    LocalDateTime startTime;
    LocalDateTime endTime;
    RuleType ruleType;
    ContestStatus contestStatus;
    ContestVisibility visibility;
    Long participantCount;
    boolean isRegistered;
    UUID authorId;
    String authorUsername;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}