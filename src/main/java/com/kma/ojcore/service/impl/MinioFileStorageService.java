package com.kma.ojcore.service.impl;

import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.service.FileStorageService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Override
    public String upload(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("File uploaded to MinIO: bucket={}, object={}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to upload file to storage.");
        }
    }

    @Override
    public void delete(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            log.info("File deleted from MinIO: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public InputStream download(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to download file from MinIO", e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "Failed to download file from storage.");
        }
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expiryInSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryInSeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL from MinIO", e);
            throw new BusinessException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Failed to generate file URL.");
        }
    }
}