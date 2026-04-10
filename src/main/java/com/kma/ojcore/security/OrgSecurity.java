package com.kma.ojcore.security;

import com.kma.ojcore.entity.Organization;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.OrgMemberStatus;
import com.kma.ojcore.enums.OrgRole;
import com.kma.ojcore.repository.OrganizationMemberRepository;
import com.kma.ojcore.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component("orgSecurity")
@RequiredArgsConstructor
public class OrgSecurity {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    /**
     * Dùng trong @PreAuthorize: kiểm tra user hiện tại có được "quản trị"
     * organization này hay không.
     * - ROLE_ADMIN: luôn được.
     * - ORG_OWNER / ORG_ADMIN (status ACTIVE) trong org đó: được.
     */
    public boolean canManageOrganization(UUID orgId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) return false;

        // Global ADMIN
        if (userPrincipal.hasRole("ROLE_ADMIN")) {
            return true;
        }

        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null || org.getStatus() != EStatus.ACTIVE) {
            return false;
        }

        UUID userId = userPrincipal.getId();

        return organizationMemberRepository.existsByOrganizationIdAndUserIdAndRoleInAndStatusAndMemberStatus(
                org.getId(),
                userId,
                Arrays.asList(OrgRole.ORG_OWNER, OrgRole.ORG_ADMIN),
                EStatus.ACTIVE,
                OrgMemberStatus.APPROVED
        );
    }
}