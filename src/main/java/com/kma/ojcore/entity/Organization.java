package com.kma.ojcore.entity;

import com.kma.ojcore.enums.OrgApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true)
    String name;

    @Column(unique = true)
    String slug; // optional, có thể auto-generate từ name

    @Column(name = "short_description")
    String shortDescription;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    OrgApprovalStatus approvalStatus = OrgApprovalStatus.UNVERIFIED;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "cover_url", length = 500)
    String coverUrl;

    @Column(name = "website_url")
    String websiteUrl;

    // -- Relationships -- //

    // Organization - User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    User owner;

    // Organization - Members (Cascade)
    @OneToMany(mappedBy = "organization", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    List<OrganizationMember> members = new ArrayList<>();
}
