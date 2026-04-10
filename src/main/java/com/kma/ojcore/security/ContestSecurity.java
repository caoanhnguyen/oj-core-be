package com.kma.ojcore.security;

import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("contestSecurity")
@RequiredArgsConstructor
public class ContestSecurity {

    private final ContestRepository contestRepository;
    private final OrgSecurity orgSecurity;

    /**
     * Quyền "quản trị" một contest cụ thể:
     *
     * - ROLE_ADMIN hoặc ROLE_MODERATOR: luôn được.
     * - Nếu contest thuộc Organization:
     *      + ORG_OWNER hoặc ORG_ADMIN trong org đó (hoặc ADMIN/MOD) được.
     * - Nếu contest không thuộc Organization:
     *      + Author của contest (hoặc ADMIN/MOD) được.
     */
    public boolean canManageContest(UUID contestId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) return false;

        // Global ADMIN / MODERATOR
        if (userPrincipal.hasRole("ROLE_ADMIN") || userPrincipal.hasRole("ROLE_MODERATOR")) {
            return true;
        }

        Contest contest = contestRepository.findById(contestId).orElse(null);
        if (contest == null || contest.getStatus() == EStatus.DELETED) {
            return false;
        }

        // Nếu contest thuộc một Organization
        if (contest.getOrganization() != null) {
            return orgSecurity.canManageOrganization(contest.getOrganization().getId(), authentication);
        }

        // Nếu không có organization: fallback cho author
        return contest.getAuthor() != null
                && contest.getAuthor().getId().equals(userPrincipal.getId());
    }
}