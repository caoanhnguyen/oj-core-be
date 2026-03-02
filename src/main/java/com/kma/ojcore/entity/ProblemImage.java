package com.kma.ojcore.entity;

import com.kma.ojcore.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Entity để tracking tất cả các ảnh được upload cho Problem
 * Bao gồm ảnh trong description, constraints, và examples
 */
@Entity
@Table(name = "problem_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemImage extends BaseEntity {

    /**
     * MinIO object key
     * VD: "temp/abc-123.png" (temporary) hoặc "problems/uuid/image1.png"
     * (committed)
     */
    @Column(name = "object_key", nullable = false, length = 500)
    String objectKey;

    /**
     * Tên file gốc do user upload
     */
    @Column(name = "original_filename")
    String originalFilename;

    /**
     * MIME type của file (image/png, image/jpeg, etc.)
     */
    @Column(name = "content_type", length = 100)
    String contentType;

    /**
     * Kích thước file (bytes)
     */
    @Column(name = "file_size")
    Long fileSize;

    /**
     * Trạng thái của ảnh (TEMPORARY, COMMITTED, DELETED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_status", nullable = false, length = 20)
    ImageStatus imageStatus;

    /**
     * Thời điểm upload ảnh
     */
    @Column(name = "uploaded_at", nullable = false)
    LocalDateTime uploadedAt;

    /**
     * Thời điểm ảnh được commit vào Problem
     */
    @Column(name = "committed_at")
    LocalDateTime committedAt;

    // -- Relationships -- //

    /**
     * Problem mà ảnh này thuộc về
     * null nếu đang ở trạng thái TEMPORARY
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    Problem problem;

    /**
     * User đã upload ảnh này
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    User uploadedBy;
}
