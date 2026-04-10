package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.organizations.OrganizationAddMemberSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationCreateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateMemberRoleSdi;
import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo;
import com.kma.ojcore.dto.request.organizations.OrganizationJoinSdi;
import com.kma.ojcore.dto.response.organizations.OrganizationSdo;
import com.kma.ojcore.enums.OrgApprovalStatus;
import com.kma.ojcore.dto.request.organizations.OrganizationReviewJoinSdi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrganizationService {

    // ================== Public & General ==================

    Page<OrganizationBasicSdo> searchOrganizations(String keyword, OrgApprovalStatus approvalStatus, boolean isAdmin, Pageable pageable);

    // ================== Site Staff Only ==================

    void approveVerifyOrganization(UUID orgId, boolean isApproved, UUID adminId);

    // ================= Organization Owner & Admin Only ==================

    List<OrganizationJoinRequestSdo> getJoinRequests(UUID orgId, UUID currentUserId);

    void reviewJoinRequests(UUID orgId, OrganizationReviewJoinSdi request, UUID currentUserId);

    List<OrganizationMemberSdo> getMembers(UUID orgId, UUID currentUserId);

    void updateMemberRole(UUID orgId, UUID memberId, OrganizationUpdateMemberRoleSdi request, UUID currentUserId);

    void removeMember(UUID orgId, UUID memberId, UUID currentUserId);

    void updateOrganizationProfile(UUID orgId, OrganizationUpdateSdi request, UUID currentUserId);

    void requestVerifyOrganization(UUID orgId, UUID currentUserId);

    // ================= Organization Owner Only ==================

    OrganizationSdo createOrganization(OrganizationCreateSdi request, UUID currentUserId);

    List<OrganizationBasicSdo> getMyOrganizations(UUID currentUserId);

    void requestJoinOrganization(UUID orgId, OrganizationJoinSdi request, UUID currentUserId);

    void cancelJoinRequest(UUID orgId, UUID currentUserId);

    List<OrganizationJoinRequestSdo> getMyJoinRequests(UUID currentUserId);
}