package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.auth.LoginRequest;
import com.kma.ojcore.dto.request.auth.RegisterRequest;
import com.kma.ojcore.dto.request.auth.ResetPasswordRequest;
import com.kma.ojcore.dto.response.auth.JwtAuthenticationResponse;
import com.kma.ojcore.dto.response.auth.UserResponse;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.BadRequestException;

import java.util.UUID;

public interface AuthService {

    JwtAuthenticationResponse login(LoginRequest loginRequest);

    UserDetailsSdo register(RegisterRequest registerRequest);

    JwtAuthenticationResponse refreshToken(String reqRefreshTokenStr);

    void logout(String accessToken, String refreshToken);

    boolean checkEmailExists(String email);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request) throws BadRequestException;

    void sendVerificationEmail(UUID userId);

    void verifyEmail(String token) throws BadRequestException;
}
