package com.kma.ojcore.dto.request.topics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTopicSdi {

    @NotBlank(message = "Topic name is required")
    String name;

    @NotBlank(message = "Slug is required")
    String slug;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;
}
