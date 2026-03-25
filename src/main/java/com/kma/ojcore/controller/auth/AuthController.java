package com.kma.ojcore.controller.auth;

import com.kma.ojcore.dto.request.auth.ResetPasswordRequest;
import com.kma.ojcore.dto.response.auth.JwtAuthenticationResponse;
import com.kma.ojcore.dto.request.auth.LoginRequest;
import com.kma.ojcore.dto.request.auth.RegisterRequest;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.utils.TokenCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.prefix}/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final TokenCookieUtil tokenCookieUtil;

    @PostMapping("/login")
    public ApiResponse<UserDetailsSdo> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // 1. Gọi Service để xác thực và lấy cặp Token + Profile
        JwtAuthenticationResponse jwtResponse = authService.login(loginRequest);

        // 2. Set Cookie
        tokenCookieUtil.setTokenCookies(response, jwtResponse.getAccessToken(), jwtResponse.getRefreshToken());

        // 3. Chỉ trả về thông tin User (UserDetailsSdo)
        return ApiResponse.<UserDetailsSdo>builder()
                .status(200)
                .message("Login successfully!")
                .data(jwtResponse.getUser())
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<UserDetailsSdo> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDetailsSdo response =  authService.register(registerRequest);
        return ApiResponse.<UserDetailsSdo>builder()
                .status(201)
                .message("User registered successfully")
                .data(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<UserDetailsSdo> refreshToken(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse) {
        try {
            String refreshTokenStr = tokenCookieUtil.getCookieValue(httpServletRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
                throw new BadCredentialsException("Không tìm thấy Refresh Token trong Cookie");
            }

            JwtAuthenticationResponse jwtResponse = authService.refreshToken(refreshTokenStr);

            tokenCookieUtil.setTokenCookies(httpResponse, jwtResponse.getAccessToken(), jwtResponse.getRefreshToken());

            return ApiResponse.<UserDetailsSdo>builder()
                    .status(200)
                    .message("Gia hạn Token thành công!")
                    .data(jwtResponse.getUser())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            tokenCookieUtil.clearCookies(httpResponse);
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã bị thu hồi. Vui lòng đăng nhập lại!");
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. Bóc Cookie ra
        String accessToken = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        // 2. Báo cho Service biết để đưa vào Blacklist / Thu hồi
        authService.logout(accessToken, refreshToken);

        // 3. Xóa Cookie trên trình duyệt
        tokenCookieUtil.clearCookies(httpResponse);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }

    @PostMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean isRegistered = authService.checkEmailExists(email);
        return ApiResponse.<Boolean>builder()
                .status(200)
                .message("Email is " + (isRegistered ? "already registered" : "available"))
                .data(isRegistered)
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