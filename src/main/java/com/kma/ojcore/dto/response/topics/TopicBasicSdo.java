package com.kma.ojcore.dto.response.topics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicBasicSdo {
    UUID topicId;
    String name;
    String slug;
}
