package com.kma.ojcore.security;

import com.kma.ojcore.entity.Contest;
import com.kma.ojcore.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("contestSecurity")
@RequiredArgsConstructor
public class ContestSecurity {

    private final ContestRepository contestRepository;

    public boolean canManageContest(UUID contestId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal user)) return false;

        // ADMIN: quản lý mọi contest
        if (user.hasRole("ROLE_ADMIN")) {
            return true;
        }

        // ASSESSOR: chỉ contest mình tạo
        if (user.hasRole("ROLE_ASSESSOR")) {
            Contest contest = contestRepository.findById(contestId).orElse(null);
            if (contest == null) return false;
            return contest.getAuthor() != null && contest.getAuthor().getId().equals(user.getId());
        }

        // Các role khác: không được
        return false;
    }
}
