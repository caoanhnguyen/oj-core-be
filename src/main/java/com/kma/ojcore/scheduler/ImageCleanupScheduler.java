package com.kma.ojcore.scheduler;

import com.kma.ojcore.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job để tự động cleanup ảnh temporary đã hết hạn
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageCleanupScheduler {

    private final ImageStorageService imageStorageService;

    /**
     * Chạy mỗi 6 giờ để cleanup ảnh temporary quá 24 giờ
     * Cron expression: "0 0
     */

    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredImages() {
        log.info("Starting scheduled cleanup of expired temporary images");

        try {
            imageStorageService.cleanupExpiredTemporaryImages();
            log.info("Completed scheduled cleanup of expired temporary images");
        } catch (Exception e) {
            log.error("Error during scheduled image cleanup", e);
        }
    }
}
