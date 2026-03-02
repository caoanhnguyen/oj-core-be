package com.kma.ojcore.dto.request.topics;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTopicSdi {

    String name;

    String slug;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;
}
