package com.kma.ojcore.dto.response.organizations;

import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationMemberSdo {

    UUID id;             // id bản ghi OrganizationMember
    UUID userId;
    String username;
    String fullName;
    OrgRole role;
    EStatus status;
    OrgMemberStatus memberStatus;
    LocalDateTime createdDate;
}