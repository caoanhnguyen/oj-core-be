package com.kma.ojcore.repository;

import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    long countByOwnerAndStatus(User owner, EStatus status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    List<Organization> findByNameIgnoreCaseOrSlugIgnoreCase(String name, String slug);

    @Query(value = "SELECT new com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo(" +
            "o.id, o.name, o.slug, o.shortDescription, o.avatarUrl) " +
            "FROM Organization o " +
            "WHERE (:keyword IS NULL OR (LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(o.slug) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
            "AND (:approvalStatus IS NULL OR o.approvalStatus = :approvalStatus) " +
            "AND (:status IS NULL OR o.status = :status)",
            countQuery = "SELECT COUNT(o) FROM Organization o WHERE " +
            "(:keyword IS NULL OR (LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(o.slug) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!')) " +
            "AND (:approvalStatus IS NULL OR o.approvalStatus = :approvalStatus) " +
            "AND (:status IS NULL OR o.status = :status)")
    Page<OrganizationBasicSdo> searchOrganizations(@Param("keyword") String keyword,
                                                   @Param("approvalStatus") OrgApprovalStatus approvalStatus,
                                                   @Param("status") EStatus status,
                                                   Pageable pageable);
}