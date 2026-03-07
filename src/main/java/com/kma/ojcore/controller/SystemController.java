package com.kma.ojcore.controller;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
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
}
