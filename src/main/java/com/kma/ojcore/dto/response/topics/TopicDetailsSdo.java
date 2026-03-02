package com.kma.ojcore.dto.response.topics;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicDetailsSdo {

    UUID topicId;
    String name;
    String slug;
    String description;
    String status;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
    String createdBy;
    String updatedBy;
}
