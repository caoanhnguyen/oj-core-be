package com.kma.ojcore.dto.request.contests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddContestProblemSdi {

    @NotNull
    UUID problemId;

    @NotBlank
    String displayId;

    @NotNull
    Integer points;

    @NotNull
    Integer sortOrder;
}