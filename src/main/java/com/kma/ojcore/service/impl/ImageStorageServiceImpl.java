package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.response.problems.ImageUploadSdo;
import com.kma.ojcore.entity.Problem;
import com.kma.ojcore.entity.ProblemImage;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.ImageStatus;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageServiceImpl implements ImageStorageService {

    private final FileStorageService fileStorageService;
    private final ProblemImageRepository problemImageRepository;

    @Value("${minio.bucket-name:oj-data}")
    private String imagesBucket;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioUrl;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String prefix = minioUrl + "/" + imagesBucket + "/";
            if (imageUrl.startsWith(prefix)) {
                String objectKey = imageUrl.substring(prefix.length());
                fileStorageService.delete(imagesBucket, objectKey);
            }
        } catch (Exception e) {
            log.error("Failed to delete old image on MinIO: {}", imageUrl, e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        validateImageFile(file);
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
            String objectKey = folder + "/" + uniqueFilename;

            fileStorageService.upload(
                    imagesBucket, objectKey, file.getInputStream(), file.getSize(), file.getContentType());

            return minioUrl + "/" + imagesBucket + "/" + objectKey;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ImageUploadSdo uploadTemporaryImage(MultipartFile file, User uploader) {
        validateImageFile(file);
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
            String objectKey = "editor/" + uniqueFilename;

            fileStorageService.upload(
                    imagesBucket, objectKey, file.getInputStream(), file.getSize(), file.getContentType());

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

            String publicUrl = minioUrl + "/" + imagesBucket + "/" + objectKey;

            return ImageUploadSdo.builder()
                    .objectKey(objectKey)
                    .url(publicUrl)
                    .originalFilename(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .uploadedAt(imageEntity.getUploadedAt())
                    .build();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void commitImages(List<String> temporaryKeys, Problem problem) {
        if (temporaryKeys == null || temporaryKeys.isEmpty()) return;

        List<ProblemImage> tempImages = problemImageRepository.findByObjectKeyIn(temporaryKeys);
        for (ProblemImage image : tempImages) {
            image.setImageStatus(ImageStatus.COMMITTED);
            image.setCommittedAt(LocalDateTime.now());
            image.setProblem(problem);
            problemImageRepository.save(image);
        }
    }

    @Override
    @Transactional
    public void syncProblemImages(List<String> usedImageKeys, Problem problem) {
        if (usedImageKeys == null) usedImageKeys = new ArrayList<>();
        commitImages(usedImageKeys, problem);

        List<ProblemImage> existingImages = problemImageRepository.findByProblemId(problem.getId());
        for (ProblemImage image : existingImages) {
            if (!usedImageKeys.contains(image.getObjectKey())) {
                try {
                    fileStorageService.delete(imagesBucket, image.getObjectKey());
                    problemImageRepository.delete(image);
                    log.info("Deleted orphaned image completely: {}", image.getObjectKey());
                } catch (Exception e) {
                    log.error("Failed to delete orphaned image: {}", image.getObjectKey(), e);
                }
            }
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredTemporaryImages() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ProblemImage> expiredImages = problemImageRepository
                .findByImageStatusAndUploadedAtBefore(ImageStatus.TEMPORARY, cutoffTime);

        for (ProblemImage image : expiredImages) {
            try {
                fileStorageService.delete(imagesBucket, image.getObjectKey());
                problemImageRepository.delete(image);
                log.info("Cleaned up expired temporary image: {}", image.getObjectKey());
            } catch (Exception e) {
                log.error("Failed to cleanup image: {}", image.getObjectKey(), e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAllImagesForProblem(UUID problemId) {
        List<ProblemImage> images = problemImageRepository.findByProblemId(problemId);
        for (ProblemImage image : images) {
            try {
                fileStorageService.delete(imagesBucket, image.getObjectKey());
                problemImageRepository.delete(image);
            } catch (Exception e) {
                log.error("Failed to delete image: {}", image.getObjectKey(), e);
            }
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.FILE_EMPTY);
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "png";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}