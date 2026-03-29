package com.kma.ojcore.dto.response.contests;

import com.kma.ojcore.enums.EStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestProblemSdo {
    UUID id;
    UUID problemId;
    String problemSlug;
    String originalTitle;
    String displayId;
    Integer points;
    Integer sortOrder;
    EStatus status;
}