package com.kma.ojcore.dto.response.topics;

import com.kma.ojcore.enums.EStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TopicAdminSdo {

    UUID topicId;
    String name;
    String slug;
    EStatus status;
    LocalDateTime updatedDate;
    String createdBy;
}
