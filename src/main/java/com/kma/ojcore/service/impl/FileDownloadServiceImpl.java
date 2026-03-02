package com.kma.ojcore.service.impl;

import com.kma.ojcore.service.FileDownloadService;
import com.kma.ojcore.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileStorageService fileStorageService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public Resource downloadByObjectKey(String objectKey) {
        return new InputStreamResource(fileStorageService.download(bucketName, objectKey));
    }

    @Override
    public String getPresignedUrlByObjectKey(String objectKey, int expiryInSeconds) {
        return fileStorageService.getPresignedUrl(bucketName, objectKey, expiryInSeconds);
    }
}
