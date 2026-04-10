package com.kma.ojcore.dto.response.organizations;

import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationMemberSdo {

    UUID id;             // id bản ghi OrganizationMember
    UUID userId;
    String username;
    String fullName;
    OrgRole role;
    EStatus status;
    LocalDateTime createdDate;
}