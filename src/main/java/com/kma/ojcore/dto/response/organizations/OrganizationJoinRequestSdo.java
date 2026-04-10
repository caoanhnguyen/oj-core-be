package com.kma.ojcore.dto.response.organizations;

import com.kma.ojcore.enums.OrgMemberStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationJoinRequestSdo {
    // Thông tin user gửi request
    UUID userId;
    String username;
    String fullName;
    String email;

    // Thông tin organization mà user muốn join
    UUID orgId;
    String orgName;
    String orgSlug;

    // Trạng thái của request
    OrgMemberStatus status;
    String message;
    LocalDateTime requestedAt;
}
