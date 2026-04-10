package com.kma.ojcore.dto.request.organizations;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationJoinSdi {
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    String message;
}
