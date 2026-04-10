package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.OrganizationMember;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByOrganizationAndUserAndStatus(Organization org, User user, EStatus status);

    List<OrganizationMember> findByOrganizationAndStatusAndMemberStatus(Organization org, EStatus status, OrgMemberStatus memberStatus);

    List<OrganizationMember> findByUserAndStatusAndMemberStatus(User user, EStatus status, OrgMemberStatus memberStatus);

    @Query("SELECT new com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo( " +
            "o.id, o.name, o.slug, o.shortDescription, o.avatarUrl) " +
            "FROM Organization o " +
            "JOIN OrganizationMember om ON o.id = om.organization.id " +
            "WHERE om.user.id = :userId AND o.status = :status AND om.status = :status AND om.memberStatus = :memberStatus")
    List<OrganizationBasicSdo> findMyOrgs(@Param("userId") UUID userId, @Param("status") EStatus status, @Param("memberStatus") OrgMemberStatus memberStatus);

    @Modifying
    @Query("UPDATE OrganizationMember m SET m.memberStatus = :newMemberStatus, m.status = :newStatus " +
            "WHERE m.organization.id = :orgId AND m.user.id IN :userIds " +
            "AND m.status = 'ACTIVE' AND m.memberStatus = 'PENDING'")
    int bulkReviewJoinRequests(@Param("orgId") UUID orgId,
                               @Param("userIds") List<UUID> userIds,
                               @Param("newMemberStatus") OrgMemberStatus newMemberStatus,
                               @Param("newStatus") EStatus newStatus);

    @Query("SELECT COUNT(m) > 0 FROM OrganizationMember m " +
            "WHERE m.organization.id = :orgId " +
            "AND m.user.id = :userId " +
            "AND m.role IN :roles " +
            "AND m.status = :status AND m.memberStatus = :memberStatus")
    boolean existsByOrganizationIdAndUserIdAndRoleInAndStatusAndMemberStatus(
            @Param("orgId") UUID orgId,
            @Param("userId") UUID userId,
            @Param("roles") List<OrgRole> roles,
            @Param("status") EStatus status,
            @Param("memberStatus") OrgMemberStatus memberStatus
    );
}