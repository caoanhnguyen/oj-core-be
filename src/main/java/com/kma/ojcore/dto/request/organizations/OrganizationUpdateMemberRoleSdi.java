package com.kma.ojcore.dto.request.organizations;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationUpdateMemberRoleSdi {

    @NotBlank(message = "Role key can not be blank")
    String roleKey;
}