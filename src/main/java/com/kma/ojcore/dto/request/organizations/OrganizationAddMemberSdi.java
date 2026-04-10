package com.kma.ojcore.dto.request.organizations;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationAddMemberSdi {

    @NotNull(message = "User Ids can not be null")
    UUID userId;

    // Ví dụ: "ORG_ADMIN", "ORG_MEMBER"
    @NotNull(message = "Role key can not be null")
    String roleKey;
}