package com.kma.ojcore.dto.response.contests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kma.ojcore.enums.ContestStatus;
import com.kma.ojcore.enums.ContestVisibility;
import com.kma.ojcore.enums.ContestFormat;
import com.kma.ojcore.enums.RuleType;
import com.kma.ojcore.enums.ScoreboardVisibility;
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
    String contestKey;
    String description;
    LocalDateTime startTime;
    LocalDateTime endTime;
    RuleType ruleType;
    ContestStatus contestStatus;
    ContestVisibility visibility;
    Integer durationMinutes;
    ContestFormat format;
    Boolean allowLateRegistration;
    ScoreboardVisibility scoreboardVisibility;
    Long participantCount;
    @JsonProperty("isRegistered")
    boolean isRegistered;
    UUID authorId;
    String authorUsername;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;

    // Trả về contest participation của user hiện tại nếu đã đăng ký, null nếu chưa đăng ký
    ContestParticipationSdo contestParticipation;
    LocalDateTime serverTime;
}