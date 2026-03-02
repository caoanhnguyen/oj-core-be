package com.kma.ojcore.service;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * Upload một file lên object storage và trả về đường dẫn (URL hoặc object key).
     */
    String upload(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    /**
     * Xóa file khỏi object storage (nếu cần).
     */
    void delete(String bucketName, String objectName);

    /**
     * Tải file từ object storage, trả về InputStream.
     */
    InputStream download(String bucketName, String objectName);

    /**
     * Tạo presigned URL (link tạm thời) để truy cập object trực tiếp từ MinIO.
     */
    String getPresignedUrl(String bucketName, String objectName, int expiryInSeconds);
}
