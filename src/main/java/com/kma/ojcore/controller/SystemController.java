package com.kma.ojcore.controller;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("${app.api.prefix}/system")
@RequiredArgsConstructor
public class SystemController {
    private final SystemService systemService;

    @GetMapping("/languages")
    public ApiResponse<?> getLanguages() {
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .data(systemService.getSupportedLanguages())
                .build();
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'ASSESSOR')")
    public ApiResponse<?> getDashboardStats(@RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .data(systemService.getAdminDashboardStats(days))
                .build();
    }
}
