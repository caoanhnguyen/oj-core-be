package com.kma.ojcore.controller.files;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.problems.ImageUploadSdo;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller để xử lý upload và quản lý ảnh cho Problem
 */
@RestController
@RequestMapping("${app.api.prefix}/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;

    /**
     * Upload ảnh tạm thời cho Problem description/examples
     * Chỉ ADMIN mới được upload
     * Ảnh sẽ được lưu vào MinIO với prefix "temp/" và tự động cleanup sau 24h nếu
     * không được commit
     *
     * @param file          MultipartFile ảnh cần upload
     * @param userPrincipal User hiện tại (từ Security Context)
     * @return ImageUploadSdo chứa objectKey và presigned URL
     */
    @PostMapping("/upload-temp")
    public ApiResponse<ImageUploadSdo> uploadTemporaryImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Create User entity from UserPrincipal
        com.kma.ojcore.entity.User uploader = new com.kma.ojcore.entity.User();
        uploader.setId(userPrincipal.getId());
        uploader.setUsername(userPrincipal.getUsername());
        uploader.setEmail(userPrincipal.getEmail());

        ImageUploadSdo result = imageStorageService.uploadTemporaryImage(file, uploader);

        return ApiResponse.<ImageUploadSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Image uploaded successfully")
                .data(result)
                .build();
    }

    /**
     * Cleanup ảnh temporary thủ công (Admin only)
     * Xóa tất cả ảnh temporary đã quá 24 giờ
     */
    @DeleteMapping("/cleanup-temp")
    public ApiResponse<Void> cleanupTemporaryImages() {
        imageStorageService.cleanupExpiredTemporaryImages();

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Temporary images cleaned up successfully")
                .build();
    }
}
