package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationManageMemberSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.OrganizationMember;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByOrganizationAndUser_IdAndStatus(Organization org, UUID userId, EStatus status);

    List<OrganizationMember> findByOrganizationAndStatusAndMemberStatus(Organization org, EStatus status, OrgMemberStatus memberStatus);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo(" +
           "m.id, m.user.id, m.user.username, m.user.fullName, m.role, m.status, m.memberStatus, m.createdDate) " +
           "FROM OrganizationMember m " +
           "WHERE m.organization.id = :orgId " +
           "AND (:keyword IS NULL OR (LOWER(m.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:memberStatus IS NULL OR m.memberStatus = :memberStatus)",
           countQuery = "SELECT COUNT(m) FROM OrganizationMember m WHERE m.organization.id = :orgId " +
           "AND (:keyword IS NULL OR (LOWER(m.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:memberStatus IS NULL OR m.memberStatus = :memberStatus)")
    Page<OrganizationMemberSdo> searchMembers(@Param("orgId") UUID orgId,
                                              @Param("keyword") String keyword,
                                              @Param("status") EStatus status,
                                              @Param("memberStatus") OrgMemberStatus memberStatus,
                                              Pageable pageable);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.organizations.OrganizationManageMemberSdo(" +
           "m.id, m.user.id, m.user.username, m.user.fullName, m.user.email, m.role, m.status, m.memberStatus, m.createdDate, m.updatedDate) " +
           "FROM OrganizationMember m " +
           "WHERE m.organization.id = :orgId " +
           "AND (:keyword IS NULL OR (LOWER(m.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:memberStatus IS NULL OR m.memberStatus = :memberStatus) " +
           "AND (:role IS NULL OR m.role = :role)",
           countQuery = "SELECT COUNT(m) FROM OrganizationMember m WHERE m.organization.id = :orgId " +
           "AND (:keyword IS NULL OR (LOWER(m.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:memberStatus IS NULL OR m.memberStatus = :memberStatus) " +
           "AND (:role IS NULL OR m.role = :role)")
    Page<OrganizationManageMemberSdo> searchManageMembers(@Param("orgId") UUID orgId,
                                                          @Param("keyword") String keyword,
                                                          @Param("status") EStatus status,
                                                          @Param("memberStatus") OrgMemberStatus memberStatus,
                                                          @Param("role") OrgRole role,
                                                          Pageable pageable);

    Optional<OrganizationMember> findByOrganizationAndUser_Id(Organization org, UUID userId);

    @Query("SELECT new com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo( " +
            "m.user.id, m.user.username, m.user.fullName, m.user.email, " +
            "m.organization.id, m.organization.name, m.organization.slug, " +
            "m.memberStatus, m.joinRequestMessage, m.createdDate) " +
            "FROM OrganizationMember m " +
            "WHERE m.user.id = :userId " +
            "ORDER BY m.createdDate DESC")
    List<OrganizationJoinRequestSdo> findJoinReqsByUserId(UUID userId);

    @Query("SELECT new com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo( " +
            "m.user.id, m.user.username, m.user.fullName, m.user.email, " +
            "m.organization.id, m.organization.name, m.organization.slug, " +
            "m.memberStatus, m.joinRequestMessage, m.createdDate) " +
            "FROM OrganizationMember m " +
            "WHERE m.organization.id = :orgId AND m.status = :status AND m.memberStatus = :memberStatus " +
            "ORDER BY m.createdDate ASC")
    Page<OrganizationJoinRequestSdo> findJoinReqsByOrganizationIdAndStatusAndMemberStatus(@Param("orgId") UUID orgId,
                                                                                          @Param("status") EStatus status,
                                                                                          @Param("memberStatus") OrgMemberStatus memberStatus,
                                                                                          Pageable pageable);

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