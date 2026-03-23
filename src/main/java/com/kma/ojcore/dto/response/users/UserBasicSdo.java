package com.kma.ojcore.dto.response.users;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserBasicSdo {

    UUID id;
    String username;
    String email;
    String fullName;
    boolean accountNonLocked;
    Set<String> roles;

    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}