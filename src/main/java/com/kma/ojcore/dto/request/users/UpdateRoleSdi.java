package com.kma.ojcore.dto.request.users;

import com.kma.ojcore.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateRoleSdi {
    @NotEmpty(message = "Người dùng phải có ít nhất 1 quyền (ROLE_USER)")
    Set<RoleName> roles;
}
