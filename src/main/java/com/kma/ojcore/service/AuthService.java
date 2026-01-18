package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.LoginRequest;
import com.kma.ojcore.dto.request.RegisterRequest;
import com.kma.ojcore.dto.request.ResetPasswordRequest;
import com.kma.ojcore.dto.response.JwtAuthenticationResponse;
import com.kma.ojcore.dto.response.UserResponse;
import com.kma.ojcore.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.BadRequestException;

import java.util.UUID;

public interface AuthService {

    JwtAuthenticationResponse login(LoginRequest loginRequest, HttpServletResponse httpResponse);

    UserResponse register(RegisterRequest registerRequest);

    JwtAuthenticationResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    boolean checkEmailExists(String email);

    UserResponse getCurrentUser(UserPrincipal currentUser);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request) throws BadRequestException;

    void sendVerificationEmail(UUID userId);

    void verifyEmail(String token) throws BadRequestException;
}
