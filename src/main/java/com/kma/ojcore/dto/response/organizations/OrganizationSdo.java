package com.kma.ojcore.dto.response.organizations;

import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgApprovalStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationSdo {

    UUID id;
    String name;
    String slug;
    String description;
    String shortDescription;

    OrgApprovalStatus approvalStatus;
    String avatarUrl;
    String coverUrl;
    String websiteUrl;

    // thông tin cơ bản của owner
    UUID ownerId;
    String ownerUsername;

    EStatus status;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}