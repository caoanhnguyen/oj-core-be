package com.kma.ojcore.controller;

import com.kma.ojcore.dto.request.ResetPasswordRequest;
import com.kma.ojcore.dto.response.JwtAuthenticationResponse;
import com.kma.ojcore.dto.request.LoginRequest;
import com.kma.ojcore.dto.request.RegisterRequest;
import com.kma.ojcore.dto.response.UserResponse;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller:
 * Access token được set vào cookie (httpOnly)
 * Refresh token được lưu vào database và set vào cookie (httpOnly)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                HttpServletResponse httpResponse) {
        JwtAuthenticationResponse response = authService.login(loginRequest, httpResponse);
        return ApiResponse.<JwtAuthenticationResponse>builder()
                .status(200)
                .message("Login successful")
                .data(response)
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse response =  authService.register(registerRequest);
        return ApiResponse.<UserResponse>builder()
                .status(201)
                .message("User registered successfully")
                .data(response)
                .build();
    }

    @PostMapping("/refresh")
    public JwtAuthenticationResponse refreshToken(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse) {
        return authService.refreshToken(httpServletRequest, httpResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }

    // Kiểm tra email đã được đăng ký chưa
    @PostMapping("/check-email")
    public ApiResponse<?> checkEmail(@RequestParam String email) {
        boolean isRegistered = authService.checkEmailExists(email);
        return ApiResponse.builder()
                .status(200)
                .message("Email is " + (isRegistered ? "already registered" : "available"))
                .data(isRegistered)
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

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ApiResponse.builder()
                .status(200)
                .message("Nếu email tồn tại trong hệ thống, một mã OTP đã được gửi để đặt lại mật khẩu.")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) throws BadRequestException {
        authService.resetPassword(request);
        return ApiResponse.builder()
                .status(200)
                .message("Mật khẩu đã được đặt lại thành công.")
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse<?> verifyEmail(@RequestParam String token) throws BadRequestException {
        authService.verifyEmail(token);
        return ApiResponse.builder()
                .status(200)
                .message("Email đã được xác thực thành công. Bạn có thể đăng nhập ngay bây giờ.")
                .build();
    }

    @PostMapping("/resend-verification-email")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> resendVerificationEmail(@AuthenticationPrincipal UserPrincipal currentUser) {
        authService.sendVerificationEmail(currentUser.getId());
        return ApiResponse.builder()
                .status(200)
                .message("Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư của bạn.")
                .build();
    }
}

