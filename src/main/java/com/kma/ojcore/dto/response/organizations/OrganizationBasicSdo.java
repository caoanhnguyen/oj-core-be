package com.kma.ojcore.dto.response.organizations;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationBasicSdo {

    UUID orgId;
    String name;
    String slug;
    String shortDescription;
    String avatarUrl;

    // TODO: sẽ thêm trường score và members_count sau
}
