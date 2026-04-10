package com.kma.ojcore.dto.request.organizations;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationUpdateSdi {

    @NotBlank(message = "Organization name can not be blank")
    String name;

    @NotBlank(message = "Slug can not be blank")
    String slug;

    String description;
    String shortDescription;
    String websiteUrl;
    String avatarUrl;
    String coverUrl;
}
