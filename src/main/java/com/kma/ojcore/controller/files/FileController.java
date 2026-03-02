package com.kma.ojcore.controller.files;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadService fileDownloadService;

    /**
     * Download file từ MinIO dựa trên object key đã lưu trong DB
     * (inputUrl/outputUrl/illustrationUrl).
     */
    @GetMapping("/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("key") String objectKey) {
        try {
            Resource resource = fileDownloadService.downloadByObjectKey(objectKey);
            InputStreamResource streamResource = new InputStreamResource(resource.getInputStream());

            String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);

            MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(streamResource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Trả về presigned URL để FE có thể load trực tiếp từ MinIO.
     * FE truyền objectKey (chính là inputUrl/outputUrl/illustrationUrl) và thời
     * gian sống (giây).
     */
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> getPresignedUrl(@RequestParam("key") String objectKey,
            @RequestParam(value = "expiry", defaultValue = "7200") int expiryInSeconds) {
        String url = fileDownloadService.getPresignedUrlByObjectKey(objectKey, expiryInSeconds);
        return ApiResponse.<String>builder()
                .status(200)
                .message("Get presigned URL successfully")
                .data(url)
                .build();
    }

    /**
     * Endpoint public để xem ảnh (render inline)
     * URL này sẽ được nhúng vào thẻ <img src="...">
     */
    @GetMapping("/view")
    public ResponseEntity<InputStreamResource> viewFile(@RequestParam("key") String objectKey) {
        try {
            Resource resource = fileDownloadService.downloadByObjectKey(objectKey);
            InputStreamResource streamResource = new InputStreamResource(resource.getInputStream());

            String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
            MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                    .orElse(MediaType.IMAGE_PNG);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(streamResource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
