package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.problems.ImageUploadSdo;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service để quản lý lifecycle của ảnh trong hệ thống
 */
public interface ImageStorageService {

    /**
     * Upload ảnh tạm thời (temporary) vào MinIO với prefix "temp/"
     * Ảnh sẽ được lưu vào bucket "oj-images" với path: temp/{uuid}.{extension}
     *
     * @param file     Multipart file từ request
     * @param uploader User đang upload
     * @return ImageUploadSdo chứa objectKey và presigned URL
     */
    ImageUploadSdo uploadTemporaryImage(MultipartFile file, User uploader);

    /**
     * Commit các ảnh temporary thành committed cho một Problem
     * Di chuyển từ "temp/{uuid}.ext" sang "problems/{problemId}/{uuid}.ext"
     *
     * @param temporaryKeys Danh sách object keys temporary (VD: ["temp/abc.png"])
     * @param problem       Problem được gắn ảnh
     */
    void commitImages(List<String> temporaryKeys, Problem problem);

    /**
     * Xóa các ảnh temporary đã quá hạn (> 24 giờ)
     * Được gọi bởi scheduled job
     */
    void cleanupExpiredTemporaryImages();

        /**
        * Đồng bộ ảnh khi cập nhật Problem
        * - Xóa những ảnh không còn được sử dụng (không có trong usedImageKeys)
        * - Commit những ảnh mới được thêm vào (có trong usedImageKeys nhưng chưa commit)
        *
        * @param usedImageKeys Danh sách object keys đang được sử dụng trong Problem (có thể là temporary hoặc committed)
        * @param problem       Problem đang được cập nhật
        */
    void syncProblemImages(List<String> usedImageKeys, Problem problem);

    /**
     * Xóa tất cả ảnh của một Problem (khi delete Problem)
     *
     * @param problemId UUID của Problem
     */
    void deleteAllImagesForProblem(UUID problemId);
}
