package com.kma.ojcore.controller;

import com.kma.ojcore.dto.response.UserResponse;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.service.UserService;
import com.kma.ojcore.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Get user by ID successful")
                .data(response)
                .build();
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse response = userService.getUserByUsername(username);
        return ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Get user by username successful")
                .data(response)
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse userResponse = authService.getCurrentUser(currentUser);
        return ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Get current user successful")
                .data(userResponse)
                .build();
    }
}

