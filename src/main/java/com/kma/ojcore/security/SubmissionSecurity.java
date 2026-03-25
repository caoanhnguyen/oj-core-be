package com.kma.ojcore.security;

import com.kma.ojcore.entity.Submission;
import com.kma.ojcore.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("submissionSecurity") // Tên này phải match chính xác với tên trong @PreAuthorize
@RequiredArgsConstructor
public class SubmissionSecurity {

    private final SubmissionRepository submissionRepository;

    public boolean isSubmissionOwnerOrAdmin(UUID submissionId, Authentication authentication) {
        // 1. Nếu chưa đăng nhập -> Chặn luôn (403 Forbidden)
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 2. Kiểm tra xem user có phải là ADMIN hoặc MODERATOR không
        // Chú ý: Spring Security thường tự thêm tiền tố "ROLE_" vào role.
        // Tuỳ cấu hình JWT của bro mà ở đây check có hoặc không có tiền tố nhé!
        boolean isAdminOrMod = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MODERATOR"));

        if (isAdminOrMod) {
            return true; // Cho qua luôn, không cần check chủ sở hữu
        }

        // 3. Nếu là User thường, check xem có phải chủ sở hữu không
        // Tìm submission trong DB
        Submission submission = submissionRepository.findById(submissionId)
                .orElse(null);

        // Nếu submission không tồn tại, trả về false (Spring sẽ ném 403).
        // Hoặc bro có thể throw ResourceNotFoundException ở đây để nó ném 404 cho tường minh.
        if (submission == null) {
            return false;
        }

        // 4. Lấy ID của user đang đăng nhập từ đối tượng Authentication
        UUID currentUserId = extractUserId(authentication);

        // So sánh ID của người nộp bài với ID của người đang request
        return submission.getUser().getId().equals(currentUserId);
    }

    // Hàm helper để moi cái UUID ra khỏi Authentication
    private UUID extractUserId(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();

        } catch (Exception e) {
            log.error("Lỗi khi parse ID người dùng từ Authentication", e);
            return null;
        }
    }
}