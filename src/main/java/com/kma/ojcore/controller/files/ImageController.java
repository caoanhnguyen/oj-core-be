package com.kma.ojcore.controller.files;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ImageUploadSdo;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${app.api.prefix}/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final UserRepository userRepository;

    /**
     * API upload ảnh tạm thời cho trình soạn thảo (editor).
     * FE sẽ gọi API này khi người dùng chèn ảnh vào trình soạn thảo.
     * Ảnh sẽ được lưu tạm thời với trạng thái "TEMPORARY" và có thể được xóa sau 24h nếu không được sử dụng.
     * Trả về URL tạm thời từ minIO để FE có thể hiển thị ngay trong editor.
     */
    @PostMapping("/editor/upload")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERATOR')")
    public ApiResponse<ImageUploadSdo> uploadEditorImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User uploader = userRepository.getReferenceById(userPrincipal.getId());

        ImageUploadSdo result = imageStorageService.uploadTemporaryImage(file, uploader);

        return ApiResponse.<ImageUploadSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Image uploaded successfully")
                .data(result)
                .build();
    }

    /**
     * API dọn dẹp ảnh tạm thời đã hết hạn (hết 24h).
     * Có thể được gọi định kỳ bằng cron job hoặc scheduler.
     */
    @DeleteMapping("/editor/cleanup")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> cleanupTemporaryImages() {
        imageStorageService.cleanupExpiredTemporaryImages();

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Temporary images cleaned up successfully")
                .build();
    }
}