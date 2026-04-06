package com.kma.ojcore.security;

import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("submissionSecurity")
@RequiredArgsConstructor
public class SubmissionSecurity {

    private final SubmissionRepository submissionRepository;

    public boolean isSubmissionOwnerOrAdmin(UUID submissionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdminOrMod = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MODERATOR"));

        if (isAdminOrMod) {
            return true;
        }

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (submission.getStatus() != EStatus.ACTIVE) {
            return false;
        }

        UUID currentUserId = extractUserId(authentication);
        return submission.getUser().getId().equals(currentUserId);
    }

    private UUID extractUserId(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        } catch (Exception e) {
            log.error("Failed to parse user ID from Authentication", e);
            return null;
        }
    }
}