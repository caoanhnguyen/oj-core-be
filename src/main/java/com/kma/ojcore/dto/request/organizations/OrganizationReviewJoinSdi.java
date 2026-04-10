package com.kma.ojcore.dto.request.organizations;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationReviewJoinSdi {
    @NotEmpty(message = "User IDs list cannot be empty")
    List<UUID> userIds;

    @NotNull(message = "isApproved flag cannot be null")
    Boolean isApproved;
}
