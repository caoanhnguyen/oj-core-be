package com.kma.ojcore.repository;

import com.kma.ojcore.entity.ProblemImage;
import com.kma.ojcore.enums.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository để quản lý ProblemImage
 */
@Repository
public interface ProblemImageRepository extends JpaRepository<ProblemImage, UUID> {

    /**
     * Tìm các ảnh theo danh sách object keys
     * Dùng để commit images từ temporary sang committed
     */
    List<ProblemImage> findByObjectKeyIn(List<String> objectKeys);

    /**
     * Tìm các ảnh temporary đã quá hạn để cleanup
     * 
     * @param status     Trạng thái ảnh (thường là TEMPORARY)
     * @param cutoffTime Thời điểm cutoff (VD: NOW() - 24h)
     */
    List<ProblemImage> findByImageStatusAndUploadedAtBefore(
            ImageStatus status,
            LocalDateTime cutoffTime);

    /**
     * Tìm tất cả ảnh của một Problem
     * Dùng khi xóa Problem
     */
    List<ProblemImage> findByProblemId(UUID problemId);

    /**
     * Tìm ảnh theo object key
     */
    ProblemImage findByObjectKey(String objectKey);
}
