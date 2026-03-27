package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.auth.LoginRequest;
import com.kma.ojcore.dto.request.auth.RegisterRequest;
import com.kma.ojcore.dto.request.auth.ResetPasswordRequest;
import com.kma.ojcore.dto.response.auth.EmailMessage;
import com.kma.ojcore.dto.response.auth.JwtAuthenticationResponse;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.Provider;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.UserMapper;
import com.kma.ojcore.repository.RoleRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.CustomUserDetailsService;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.security.jwt.JwtTokenProvider;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.service.RefreshTokenService;
import com.kma.ojcore.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String, String> redisTemplate;
    private final CustomUserDetailsService customUserDetailsService;
    private final RabbitTemplate rabbitTemplate;

    private static final String RESET_PASS_PREFIX = "RESET_PASS:";
    private static final String VERIFY_EMAIL_PREFIX = "VERIFY_EMAIL:";
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Transactional
    @Override
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userPrincipal.getId());

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            UserDetailsSdo userDetailsSdo = userMapper.toUserDetailsSdo(user, true);

            return JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .user(userDetailsSdo)
                    .build();

        } catch (LockedException e) {
            log.warn("Login failed: Account locked for user {}", loginRequest.getUsername());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);

        } catch (DisabledException e) {
            log.warn("Login failed: Email not verified for user {}", loginRequest.getUsername());
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);

        } catch (BadCredentialsException e) {
            log.warn("Login failed: Wrong credentials for user {}", loginRequest.getUsername());
            throw new BusinessException(ErrorCode.WRONG_CREDENTIALS);

        } catch (Exception e) {
            log.error("Login failed due to unexpected error: {}", e.getMessage());
            throw new BusinessException(ErrorCode.WRONG_CREDENTIALS);
        }
    }

    @Transactional
    @Override
    public UserDetailsSdo register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .provider(Provider.LOCAL)
                .emailVerified(false)
                .build();
        user.setStatus(EStatus.INACTIVE);

        Role userRole = roleRepository.getUserRole();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        try {
            sendVerificationEmail(savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }

        return userMapper.toUserDetailsSdo(user, true);
    }

    @Transactional
    @Override
    public JwtAuthenticationResponse refreshToken(String reqRefreshTokenStr) {
        RefreshToken oldRefreshToken = refreshTokenService.findByToken(reqRefreshTokenStr)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        oldRefreshToken = refreshTokenService.verifyExpiration(oldRefreshToken);
        User user = oldRefreshToken.getUser();

        // Check Redis
        UserDetails userDetails = customUserDetailsService.loadUserById(user.getId());
        UserPrincipal principal = (UserPrincipal) userDetails;

        if (!principal.isAccountNonLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        String newAccessToken = tokenProvider.generateAccessToken(principal);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
        UserDetailsSdo userDetailsSdo = userMapper.toUserDetailsSdo(user, true);

        return JwtAuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .user(userDetailsSdo)
                .build();
    }

    @Transactional
    @Override
    public void logout(String accessToken, String refreshToken) {
        try {
            if (refreshToken != null && !refreshToken.isBlank()) {
                refreshTokenService.revokeToken(refreshToken);
            }
            if (accessToken != null && !accessToken.isBlank()) {
                tokenBlacklistService.blacklistToken(accessToken);
            }
        } catch (Exception e) {
            log.warn("Có lỗi xảy ra khi revoke/blacklist token lúc logout: {}", e.getMessage());
        }
    }

    @Override
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        String otp = String.valueOf(secureRandom.nextInt(900000) + 100000);

        redisTemplate.opsForValue().set(
                RESET_PASS_PREFIX + normalizedEmail,
                otp,
                5,
                TimeUnit.MINUTES
        );

        EmailMessage message = EmailMessage.builder()
                .to(normalizedEmail)
                .subject("Khôi phục mật khẩu - OJ Core")
                .content("<h1>Mã xác thực của bạn là: " + otp + "</h1><p>Mã này sẽ hết hạn sau 5 phút.</p>")
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, message);
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String cachedOtp = redisTemplate.opsForValue().get(RESET_PASS_PREFIX + normalizedEmail);

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid or expired OTP.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user); // Đuổi sạch thiết bị cũ
        redisTemplate.delete("userDetails::" + user.getId());  // Ép cập nhật version trên Redis

        redisTemplate.delete(RESET_PASS_PREFIX + normalizedEmail);
    }

    @Override
    public void sendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email is already verified.");
        }

        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        redisTemplate.opsForValue().set(
                VERIFY_EMAIL_PREFIX + token,
                userId.toString(),
                24,
                TimeUnit.HOURS
        );

        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        EmailMessage message = EmailMessage.builder()
                .to(user.getEmail())
                .subject("Xác thực Email - OJ Core")
                .content("<h1>Chào " + user.getFullName() + ",</h1>" +
                        "<p>Vui lòng click vào link bên dưới để xác thực tài khoản của bạn:</p>" +
                        "<a href=\"" + verificationLink + "\">Xác thực Email ngay</a>" +
                        "<p>Link này sẽ hết hạn sau 24 giờ.</p>")
                .build();

        // Bắn RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, message);
    }

    @Transactional
    @Override
    public void verifyEmail(String token) {
        String userIdStr = redisTemplate.opsForValue().get(VERIFY_EMAIL_PREFIX + token);

        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Verification link is invalid or expired.");
        }

        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email has already been verified.");
        }

        user.setEmailVerified(true);
        user.setStatus(EStatus.ACTIVE);
        userRepository.save(user);

        redisTemplate.delete(VERIFY_EMAIL_PREFIX + token);
    }
}