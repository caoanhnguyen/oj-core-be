package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.organizations.OrganizationCreateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateMemberRoleSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationJoinSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationReviewJoinSdi;
import com.kma.ojcore.dto.response.organizations.*;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.OrganizationMember;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgApprovalStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.utils.EscapeHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.kma.ojcore.mapper.OrganizationMapper;
import com.kma.ojcore.repository.OrganizationMemberRepository;
import com.kma.ojcore.repository.OrganizationRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;

    // ================== Public & General operations ==================

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationBasicSdo> searchOrganizations(String keyword, OrgApprovalStatus approvalStatus, boolean isAdmin, Pageable pageable) {
        EStatus statusFilter = isAdmin ? null : EStatus.ACTIVE;
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        return organizationRepository.searchOrganizations(searchKeyword, approvalStatus, statusFilter, pageable);
    }

    // ================== Site Staff operations only ==================

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void approveVerifyOrganization(UUID orgId, boolean isApproved, UUID adminId) {
        Organization org = findActiveOrganization(orgId);

        if (org.getApprovalStatus() != OrgApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Organization is not pending verification.");
        }

        org.setApprovalStatus(isApproved ? OrgApprovalStatus.VERIFIED : OrgApprovalStatus.REJECTED);
        organizationRepository.save(org);
        log.info("Organization [{}] verification [{}] by admin [{}]",
                org.getName(), isApproved ? "APPROVED" : "REJECTED", adminId);
    }

    // ================== Organization Owner & Admin operations only ==================

    @Override
    @Transactional(readOnly = true)
    // TODO: maybe là có thêm phân trang
    public Page<OrganizationJoinRequestSdo> getJoinRequests(UUID orgId, UUID currentUserId, Pageable pageable) {
        Organization org = findActiveOrganization(orgId);

        return organizationMemberRepository.findJoinReqsByOrganizationIdAndStatusAndMemberStatus(
                org.getId(), EStatus.ACTIVE, OrgMemberStatus.PENDING, pageable);
    }

    @Override
    @Transactional
    public void reviewJoinRequests(UUID orgId, OrganizationReviewJoinSdi request, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        List<UUID> userIds = request.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        OrgMemberStatus newMemberStatus = Boolean.TRUE.equals(request.getIsApproved()) ? OrgMemberStatus.APPROVED : OrgMemberStatus.REJECTED;
        EStatus newStatus = Boolean.TRUE.equals(request.getIsApproved()) ? EStatus.ACTIVE : EStatus.DELETED;

        int updatedCount = organizationMemberRepository.bulkReviewJoinRequests(org.getId(), userIds, newMemberStatus, newStatus);

        if (updatedCount != userIds.size()) {
            log.warn("Bulk update join requests mismatch. Expected to update {} but only updated {}. Org: {}, Admin: {}", 
                     userIds.size(), updatedCount, orgId, currentUserId);
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Some join requests could not be processed. Please verify the user IDs and their current request status.");
        }
        
        log.info("Successfully reviewed {} join requests for organization [{}]. Approved: {}", 
                 updatedCount, org.getName(), request.getIsApproved());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationMemberSdo> getMembers(UUID orgId, String keyword, boolean isStaff, Pageable pageable) {
        Organization org = findActiveOrganization(orgId);

        // Dù là Staff hay User thường, danh sách Members chính thức của 1 Org luôn luôn chỉ gồm những người đã được APPROVED.
        // Các trạng thái PENDING thì nằm bên API getJoinRequests.
        EStatus statusFilter = EStatus.ACTIVE;
        OrgMemberStatus memberStatusFilter = OrgMemberStatus.APPROVED;
        
        String searchKeyword = EscapeHelper.escapeLike(keyword);

        return organizationMemberRepository.searchMembers(org.getId(), searchKeyword, statusFilter, memberStatusFilter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationManageMemberSdo> getMembersForManagement(UUID orgId, String keyword, EStatus status, OrgMemberStatus memberStatus, OrgRole role, Pageable pageable) {
        Organization org = findActiveOrganization(orgId);
        String searchKeyword = EscapeHelper.escapeLike(keyword);
        return organizationMemberRepository.searchManageMembers(org.getId(), searchKeyword, status, memberStatus, role, pageable);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    // TODO: xem lại nghiệp vụ có được gán role owner cho người khác không, hay chỉ có thể gán admin thôi? Cần valid chặt hơn như là ORG luôn phải có 1 owner, api này chỉ cho phép owner thao tác gán quyền admin và member qua lại.
    public void updateMemberRole(UUID orgId, UUID memberId, OrganizationUpdateMemberRoleSdi request, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        OrganizationMember member = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_MEMBER_NOT_FOUND));

        if (!member.getOrganization().getId().equals(org.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Member does not belong to this organization.");
        }

        if (member.getStatus() != EStatus.ACTIVE || member.getMemberStatus() != OrgMemberStatus.APPROVED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Member is not active or approved.");
        }

        OrgRole newRole = parseOrgRole(request.getRoleKey());
        member.setRole(newRole);
        organizationMemberRepository.save(member);

        log.info("Updated role of user [{}] in organization [{}] to [{}] by [{}]",
                member.getUser().getUsername(), org.getName(), newRole, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    // TODO: xem lại nghiệp vụ, có thể cho remove bulk. và khả năng là xóa bản ghi member luôn. Lưu ý cascade nếu xóa member thì set null các ràng buộc khóa ngoại ở các bảng khác (nếu có).
    public void removeMember(UUID orgId, UUID memberId, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        OrganizationMember member = organizationMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORG_MEMBER_NOT_FOUND));

        if (!member.getOrganization().getId().equals(org.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Member does not belong to this organization.");
        }

        if (member.getRole() == OrgRole.ORG_OWNER) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cannot remove organization owner via this operation.");
        }

        if (member.getStatus() != EStatus.ACTIVE || member.getMemberStatus() != OrgMemberStatus.APPROVED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Member is not active or approved.");
        }

        organizationMemberRepository.delete(member);

        log.info("Removed user [{}] from organization [{}] by [{}]",
                member.getUser().getUsername(), org.getName(), currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrganizationSdo updateOrganizationProfile(UUID orgId, OrganizationUpdateSdi request, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        // Name trùng (nếu đổi)
        if (!org.getName().equalsIgnoreCase(request.getName())) {
            if (organizationRepository.existsByNameIgnoreCase(request.getName())) {
                throw new BusinessException(ErrorCode.ORGANIZATION_NAME_EXISTS);
            }
            org.setName(request.getName());
        }

        // Slug trùng (nếu đổi, slug từ request, nếu blank thì giữ slug cũ)
        String newSlug = request.getSlug();
        if (newSlug != null && !newSlug.isBlank()
                && !newSlug.equalsIgnoreCase(org.getSlug())) {

            if (organizationRepository.existsBySlugIgnoreCase(newSlug)) {
                throw new BusinessException(ErrorCode.ORGANIZATION_SLUG_EXISTS);
            }
            org.setSlug(newSlug);
        }

        org.setDescription(request.getDescription());
        org.setShortDescription(request.getShortDescription());
        org.setAvatarUrl(request.getAvatarUrl());
        org.setCoverUrl(request.getCoverUrl());
        org.setWebsiteUrl(request.getWebsiteUrl());

        organizationRepository.save(org);
        log.info("Organization [{}] profile updated by user [{}]", org.getName(), currentUserId);
        return organizationMapper.toSdo(org);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void requestVerifyOrganization(UUID orgId, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        if (org.getApprovalStatus() == OrgApprovalStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Organization is already verified.");
        }
        if (org.getApprovalStatus() == OrgApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Organization verification is already pending.");
        }

        org.setApprovalStatus(OrgApprovalStatus.PENDING);
        organizationRepository.save(org);
        log.info("Organization [{}] verification requested by user [{}]",
                org.getName(), currentUserId);
    }

    // ================== All roles operations only ==================

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public OrganizationSdo createOrganization(OrganizationCreateSdi request, UUID currentUserId) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long ownedCount = organizationRepository.countByOwnerAndStatus(owner, EStatus.ACTIVE);
        if (ownedCount >= 3) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "You can only own a maximum of 3 organizations.");
        }

        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getName());
        }

        List<Organization> existingOrgs = organizationRepository.findByNameIgnoreCaseOrSlugIgnoreCase(request.getName(), slug);
        for (Organization existing : existingOrgs) {
            if (existing.getName().equalsIgnoreCase(request.getName())) {
                throw new BusinessException(ErrorCode.ORGANIZATION_NAME_EXISTS);
            }
            if (existing.getSlug().equalsIgnoreCase(slug)) {
                throw new BusinessException(ErrorCode.ORGANIZATION_SLUG_EXISTS);
            }
        }

        Organization org = organizationMapper.toEntity(request);
        org.setOwner(owner);
        org.setStatus(EStatus.ACTIVE);
        org.setApprovalStatus(OrgApprovalStatus.UNVERIFIED);
        org.setSlug(slug);

        // Chuẩn bị User làm ORG_OWNER
        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(org)
                .user(owner)
                .role(OrgRole.ORG_OWNER)
                .build();
        ownerMember.setStatus(EStatus.ACTIVE);
        
        // Thêm member vào List của cha (Set đúng quan hệ 2 chiều)
        List<OrganizationMember> members = new ArrayList<>();
        members.add(ownerMember);
        org.setMembers(members);

        organizationRepository.save(org);

        log.info("User [{}] created organization [{}]", owner.getUsername(), org.getName());
        return organizationMapper.toSdo(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationBasicSdo> getMyOrganizations(UUID currentUserId) {
        return organizationMemberRepository.findMyOrgs(currentUserId, EStatus.ACTIVE, OrgMemberStatus.APPROVED);
    }

    @Override
    @Transactional
    public void requestJoinOrganization(UUID orgId, OrganizationJoinSdi request, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Query KHÔNG dùng trạng thái để tìm được cả request REJECTED (DELETED) cũ lôi lên dùng lại.
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationAndUser_Id(org, user.getId()).orElse(null);

        if (member != null) {
            if (member.getMemberStatus() == OrgMemberStatus.PENDING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You already have a pending join request.");
            }
            if (member.getMemberStatus() == OrgMemberStatus.APPROVED) {
                throw new BusinessException(ErrorCode.ORG_MEMBER_ALREADY_EXISTS, "You are already a member of this organization.");
            }
            
            // Tái sử dụng Request cũ (bị Rejected)
            member.setMemberStatus(OrgMemberStatus.PENDING);
            member.setStatus(EStatus.ACTIVE);
            member.setJoinRequestMessage(request.getMessage());
            organizationMemberRepository.save(member);
            return;
        }

        OrganizationMember newRequest = OrganizationMember.builder()
                .organization(org)
                .user(user)
                .role(OrgRole.ORG_MEMBER)
                .joinRequestMessage(request.getMessage())
                .build();
        
        newRequest.setMemberStatus(OrgMemberStatus.PENDING);
        newRequest.setStatus(EStatus.ACTIVE);
        
        organizationMemberRepository.save(newRequest);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void cancelJoinRequest(UUID orgId, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);
        if(!userRepository.existsById(currentUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationAndUser_IdAndStatus(org, currentUserId, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Join request not found."));

        if (member.getMemberStatus() != OrgMemberStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only pending join requests can be cancelled.");
        }

        organizationMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationJoinRequestSdo> getMyJoinRequests(UUID currentUserId) {
        if (!userRepository.existsById(currentUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return organizationMemberRepository.findJoinReqsByUserId(currentUserId);
    }

    // ================== Helper methods ==================

    private Organization findActiveOrganization(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));
        if (org.getStatus() != EStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND);
        }
        return org;
    }

    private OrgRole parseOrgRole(String roleKey) {
        try {
            return OrgRole.valueOf(roleKey);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.ORG_ROLE_INVALID,
                    "Invalid organization role: " + roleKey);
        }
    }

    private String generateSlug(String name) {
        if (name == null) return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}