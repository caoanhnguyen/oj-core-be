package com.kma.ojcore.service.impl;

import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.service.FileDownloadService;
import com.kma.ojcore.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileStorageService fileStorageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public Resource downloadByObjectKey(String objectKey) {
        try {
            return new InputStreamResource(fileStorageService.download(bucketName, objectKey));
        } catch (Exception e) {
            log.error("Failed to download file {}: {}", objectKey, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public String getPresignedUrlByObjectKey(String objectKey, int expiryInSeconds) {
        try {
            return fileStorageService.getPresignedUrl(bucketName, objectKey, expiryInSeconds);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", objectKey, e.getMessage());
            throw new BusinessException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Failed to generate file URL.");
        }
    }
}