package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.organizations.OrganizationCreateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateMemberRoleSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationJoinSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationReviewJoinSdi;
import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationSdo;
import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.entity.OrganizationMember;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgApprovalStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return organizationRepository.searchOrganizations(keyword, approvalStatus, statusFilter, pageable);
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
    public List<OrganizationJoinRequestSdo> getJoinRequests(UUID orgId, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        List<OrganizationMember> pendingMembers = organizationMemberRepository.findByOrganizationAndStatusAndMemberStatus(
                org, EStatus.ACTIVE, OrgMemberStatus.PENDING);

        return pendingMembers.stream()
                .map(organizationMapper::toJoinRequestSdo)
                .collect(Collectors.toList());
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
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Một số yêu cầu gia nhập không hợp lệ hoặc đã không còn ở trạng thái chờ duyệt.");
        }
        
        log.info("Successfully reviewed {} join requests for organization [{}]. Approved: {}", 
                 updatedCount, org.getName(), request.getIsApproved());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationMemberSdo> getMembers(UUID orgId, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);

        List<OrganizationMember> members =
                organizationMemberRepository.findByOrganizationAndStatusAndMemberStatus(org, EStatus.ACTIVE, OrgMemberStatus.APPROVED);

        return members.stream()
                .map(organizationMapper::toMemberSdo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
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

        member.setStatus(EStatus.DELETED);
        member.setMemberStatus(OrgMemberStatus.REJECTED);
        organizationMemberRepository.save(member);

        log.info("Removed user [{}] from organization [{}] by [{}]",
                member.getUser().getUsername(), org.getName(), currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateOrganizationProfile(UUID orgId, OrganizationUpdateSdi request, UUID currentUserId) {
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

        // Check trùng name
        if (organizationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException(ErrorCode.ORGANIZATION_NAME_EXISTS);
        }

        // Slug lấy từ request; nếu null/blank thì tự generate fallback
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getName());
        }

        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new BusinessException(ErrorCode.ORGANIZATION_SLUG_EXISTS);
        }

        Organization org = organizationMapper.toEntity(request);
        org.setOwner(owner);
        org.setStatus(EStatus.ACTIVE); //
        org.setApprovalStatus(OrgApprovalStatus.UNVERIFIED);
        org.setSlug(slug);

        organizationRepository.save(org);

        // Gán user tạo ORG là ORG_OWNER
        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(org)
                .user(owner)
                .role(OrgRole.ORG_OWNER)
                .build();
        ownerMember.setStatus(EStatus.ACTIVE);
        organizationMemberRepository.save(ownerMember);

        log.info("User [{}] created organization [{}]", owner.getUsername(), org.getName());
        return organizationMapper.toSdo(org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationBasicSdo> getMyOrganizations(UUID currentUserId) {
        return organizationMemberRepository.findMyOrgs(currentUserId, EStatus.ACTIVE, OrgMemberStatus.APPROVED);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void requestJoinOrganization(UUID orgId, OrganizationJoinSdi request, UUID currentUserId) {
        Organization org = findActiveOrganization(orgId);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationAndUserAndStatus(org, user, EStatus.ACTIVE).orElse(null);

        if (member != null) {
            if (member.getMemberStatus() == OrgMemberStatus.PENDING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "You already have a pending join request.");
            }
            if (member.getMemberStatus() == OrgMemberStatus.APPROVED) {
                throw new BusinessException(ErrorCode.ORG_MEMBER_ALREADY_EXISTS, "You are already a member of this organization.");
            }
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
        User user = userRepository.findById(currentUserId).orElse(null);

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationAndUserAndStatus(org, user, EStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Join request not found."));

        if (member.getMemberStatus() != OrgMemberStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only pending join requests can be cancelled.");
        }

        organizationMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationJoinRequestSdo> getMyJoinRequests(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return organizationMemberRepository.findByUserAndStatusAndMemberStatus(
                user, EStatus.ACTIVE, OrgMemberStatus.PENDING).stream()
                .map(organizationMapper::toJoinRequestSdo)
                .collect(Collectors.toList());
    }

    // ================== Helper methods ==================

    private Organization findActiveOrganization(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));
        if (org.getStatus() != EStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ORGANIZATION_INACTIVE);
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