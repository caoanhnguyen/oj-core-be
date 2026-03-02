package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.response.problems.ImageUploadSdo;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.ProblemImage;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.ImageStatus;
import com.kma.ojcore.exception.InvalidDataException;
import com.kma.ojcore.exception.StorageException;
import com.kma.ojcore.repository.ProblemImageRepository;
import com.kma.ojcore.service.FileStorageService;
import com.kma.ojcore.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation của ImageStorageService
 * Quản lý lifecycle của ảnh: upload temporary → commit → cleanup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageServiceImpl implements ImageStorageService {

    private final FileStorageService fileStorageService;
    private final ProblemImageRepository problemImageRepository;

    @Value("${minio.bucket-name:oj-data}")
    private String imagesBucket;

    // Allowed image types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    // Max file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    @Transactional
    public ImageUploadSdo uploadTemporaryImage(MultipartFile file, User uploader) {
        log.info("Uploading temporary image: {}", file.getOriginalFilename());

        // Validate file
        validateImageFile(file);

        try {
            // Generate unique filename
            String extension = getFileExtension(file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
            String objectKey = "temp/" + uniqueFilename;

            // Upload to MinIO
            fileStorageService.upload(
                    imagesBucket,
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType());

            // Save metadata to database
            ProblemImage imageEntity = ProblemImage.builder()
                    .objectKey(objectKey)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .imageStatus(ImageStatus.TEMPORARY)
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(uploader)
                    .build();

            problemImageRepository.save(imageEntity);

            problemImageRepository.save(imageEntity);

            // Return local proxy URL instead of MinIO presigned URL
            // URL format: /api/files/view?key={objectKey}
            // Frontend should prepend API base URL if needed, or browser handles relative
            // path
            String proxyUrl = "/api/files/view?key=" + objectKey;

            log.info("Temporary image uploaded successfully: {}", objectKey);

            return ImageUploadSdo.builder()
                    .objectKey(objectKey)
                    .url(proxyUrl)
                    .originalFilename(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .uploadedAt(imageEntity.getUploadedAt())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload temporary image", e);
            throw new StorageException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, String> commitImages(List<String> temporaryKeys, Problem problem) {
        if (temporaryKeys == null || temporaryKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("Committing {} images for problem: {}", temporaryKeys.size(), problem.getId());

        Map<String, String> urlMapping = new HashMap<>();

        // Find all temporary images
        List<ProblemImage> tempImages = problemImageRepository.findByObjectKeyIn(temporaryKeys);

        for (ProblemImage image : tempImages) {
            try {
                // Generate new object key: problems/{problemId}/{uuid}.{ext}
                String oldKey = image.getObjectKey();
                String filename = oldKey.substring(oldKey.lastIndexOf('/') + 1); // Extract filename from
                                                                                 // "temp/uuid.ext"
                String newKey = "problems/" + problem.getId() + "/" + filename;

                // Copy from temp to problems folder in MinIO
                copyObject(oldKey, newKey);

                // Delete old temporary object
                fileStorageService.delete(imagesBucket, oldKey);

                // Update image metadata
                image.setObjectKey(newKey);
                image.setImageStatus(ImageStatus.COMMITTED);
                image.setCommittedAt(LocalDateTime.now());
                image.setProblem(problem);

                problemImageRepository.save(image);

                // Add to mapping for URL replacement
                urlMapping.put(oldKey, newKey);

                log.info("Image committed: {} -> {}", oldKey, newKey);

            } catch (Exception e) {
                log.error("Failed to commit image: {}", image.getObjectKey(), e);
                // Continue with other images instead of failing completely
            }
        }

        return urlMapping;
    }

    @Override
    @Transactional
    public void cleanupExpiredTemporaryImages() {
        // Find images older than 24 hours
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ProblemImage> expiredImages = problemImageRepository
                .findByImageStatusAndUploadedAtBefore(ImageStatus.TEMPORARY, cutoffTime);

        log.info("Found {} expired temporary images to cleanup", expiredImages.size());

        for (ProblemImage image : expiredImages) {
            try {
                // Delete from MinIO
                fileStorageService.delete(imagesBucket, image.getObjectKey());

                // Update status to DELETED
                image.setImageStatus(ImageStatus.DELETED);
                problemImageRepository.save(image);

                log.info("Deleted expired temporary image: {}", image.getObjectKey());

            } catch (Exception e) {
                log.error("Failed to delete expired image: {}", image.getObjectKey(), e);
            }
        }

        log.info("Cleanup completed. Deleted {} images", expiredImages.size());
    }

    @Override
    @Transactional
    public void deleteAllImagesForProblem(UUID problemId) {
        List<ProblemImage> images = problemImageRepository.findByProblemId(problemId);

        log.info("Deleting {} images for problem: {}", images.size(), problemId);

        for (ProblemImage image : images) {
            try {
                // Delete from MinIO
                fileStorageService.delete(imagesBucket, image.getObjectKey());

                // Update status
                image.setImageStatus(ImageStatus.DELETED);
                problemImageRepository.save(image);

            } catch (Exception e) {
                log.error("Failed to delete image: {}", image.getObjectKey(), e);
            }
        }
    }

    // ========== Helper Methods ========== //

    /**
     * Validate image file (type and size)
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDataException("File is empty");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidDataException(
                    "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES));
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidDataException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "png"; // default
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Copy object in MinIO from source to destination
     * Note: MinIO Java SDK doesn't have direct copy, so we download and re-upload
     */
    private void copyObject(String sourceKey, String destKey) {
        try {
            // Download from source
            var inputStream = fileStorageService.download(imagesBucket, sourceKey);

            // Get original image metadata
            ProblemImage sourceImage = problemImageRepository.findByObjectKey(sourceKey);
            if (sourceImage == null) {
                throw new StorageException("Source image not found: " + sourceKey);
            }

            // Upload to destination
            fileStorageService.upload(
                    imagesBucket,
                    destKey,
                    inputStream,
                    sourceImage.getFileSize(),
                    sourceImage.getContentType());

            inputStream.close();

        } catch (Exception e) {
            log.error("Failed to copy object: {} -> {}", sourceKey, destKey, e);
            throw new StorageException("Failed to copy image: " + e.getMessage());
        }
    }
}
