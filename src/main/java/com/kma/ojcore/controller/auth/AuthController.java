package com.kma.ojcore.controller.auth;

import com.kma.ojcore.dto.request.auth.ResetPasswordRequest;
import com.kma.ojcore.dto.response.auth.JwtAuthenticationResponse;
import com.kma.ojcore.dto.request.auth.LoginRequest;
import com.kma.ojcore.dto.request.auth.RegisterRequest;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
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
        JwtAuthenticationResponse jwtResponse = authService.login(loginRequest);
        tokenCookieUtil.setTokenCookies(response, jwtResponse.getAccessToken(), jwtResponse.getRefreshToken());

        return ApiResponse.<UserDetailsSdo>builder()
                .status(HttpStatus.OK.value())
                .message("Login successful.")
                .data(jwtResponse.getUser())
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<UserDetailsSdo> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDetailsSdo response = authService.register(registerRequest);
        return ApiResponse.<UserDetailsSdo>builder()
                .status(HttpStatus.CREATED.value())
                .message("User registered successfully.")
                .data(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<UserDetailsSdo> refreshToken(HttpServletRequest httpServletRequest, HttpServletResponse httpResponse) {
        try {
            String refreshTokenStr = tokenCookieUtil.getCookieValue(httpServletRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token not found in cookies.");
            }

            JwtAuthenticationResponse jwtResponse = authService.refreshToken(refreshTokenStr);
            tokenCookieUtil.setTokenCookies(httpResponse, jwtResponse.getAccessToken(), jwtResponse.getRefreshToken());

            return ApiResponse.<UserDetailsSdo>builder()
                    .status(HttpStatus.OK.value())
                    .message("Token refreshed successfully.")
                    .data(jwtResponse.getUser())
                    .build();

        } catch (BusinessException e) {
            tokenCookieUtil.clearCookies(httpResponse);
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            tokenCookieUtil.clearCookies(httpResponse);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        authService.logout(accessToken, refreshToken);
        tokenCookieUtil.clearCookies(httpResponse);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful.")
                .build();
    }

    @PostMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean isRegistered = authService.checkEmailExists(email);
        return ApiResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .message("Email is " + (isRegistered ? "already registered" : "available"))
                .data(isRegistered)
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("If the email exists, an OTP has been sent to reset the password.")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) throws BadRequestException {
        authService.resetPassword(request);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Password has been reset successfully.")
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse<?> verifyEmail(@RequestParam String token) throws BadRequestException {
        authService.verifyEmail(token);
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Email verified successfully. You can now login.")
                .build();
    }

    @PostMapping("/resend-verification-email")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> resendVerificationEmail(@AuthenticationPrincipal UserPrincipal currentUser) {
        authService.sendVerificationEmail(currentUser.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Verification email resent. Please check your inbox.")
                .build();
    }
}