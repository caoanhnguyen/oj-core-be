package com.kma.ojcore.dto.response.problems;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestCaseSdo {
    UUID id;

    @JsonProperty("isSample")
    boolean isSample;

    @JsonProperty("isHidden")
    boolean isHidden;

    Integer orderIndex;
    String illustrationUrl;

    String inputData;
    String outputData;
    String inputUrl;
    String outputUrl;
}
