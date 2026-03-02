package com.kma.ojcore.service;

import org.springframework.core.io.Resource;

public interface FileDownloadService {

    Resource downloadByObjectKey(String objectKey);

    String getPresignedUrlByObjectKey(String objectKey, int expiryInSeconds);
}
