package com.kma.ojcore.dto.response.problems;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO khi upload ảnh tạm thời thành công
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageUploadSdo {

    /**
     * MinIO object key
     * VD: "temp/abc-123.png"
     */
    String objectKey;

    /**
     * URL để hiển thị ảnh (presigned URL hoặc public URL)
     */
    String url;

    /**
     * Tên file gốc
     */
    String originalFilename;

    /**
     * Kích thước file (bytes)
     */
    Long fileSize;

    /**
     * Thời điểm upload
     */
    LocalDateTime uploadedAt;
}
