package com.kma.ojcore.service.impl;

import com.kma.ojcore.config.RabbitMQConfig;
import com.kma.ojcore.dto.request.LoginRequest;
import com.kma.ojcore.dto.request.RegisterRequest;
import com.kma.ojcore.dto.request.ResetPasswordRequest;
import com.kma.ojcore.dto.response.EmailMessage;
import com.kma.ojcore.dto.response.JwtAuthenticationResponse;
import com.kma.ojcore.dto.response.UserResponse;
import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.EStatus;
import com.kma.ojcore.enums.Provider;
import com.kma.ojcore.exception.ResourceAlreadyExistsException;
import com.kma.ojcore.exception.ResourceNotFoundException;
import com.kma.ojcore.mapper.UserMapper;
import com.kma.ojcore.repository.RoleRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.security.jwt.JwtTokenProvider;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.service.RefreshTokenService;
import com.kma.ojcore.utils.TokenCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Service xử lý các chức năng liên quan đến xác thực và quản lý người dùng.
 */
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
    private final TokenCookieUtil tokenCookieUtil;
    private final TokenBlacklistServiceImpl tokenBlacklistServiceImpl;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String RESET_PASS_PREFIX = "RESET_PASS:";
    private static final String VERIFY_EMAIL_PREFIX = "VERIFY_EMAIL:";
    private final RabbitTemplate rabbitTemplate;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Transactional
    @Override
    public JwtAuthenticationResponse login(LoginRequest loginRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Tạo access token
            String accessToken = tokenProvider.generateAccessToken(authentication);

            // Lưu refresh token vào database
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // Set access token vào cookie
            tokenCookieUtil.setTokenCookies(httpResponse, accessToken, refreshToken.getToken());

            return JwtAuthenticationResponse.builder()
                    .userId(userPrincipal.getId())
                    .username(userPrincipal.getUsername())
                    .email(userPrincipal.getEmail())
                    .fullName(user.getFullName())
                    .build();

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Transactional
    @Override
    public UserResponse register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new ResourceAlreadyExistsException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already in use");
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .provider(Provider.LOCAL)
                .emailVerified(false)
                .build();
        user.setStatus(EStatus.INACTIVE);

        // Assign default role
        Role userRole = roleRepository.getUserRole();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Tự động gửi email xác thực
        try {
            sendVerificationEmail(savedUser.getId());
            log.info("Verification email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage(), e);
            // Không throw exception để không block quá trình đăng ký
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    @Override
    public JwtAuthenticationResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String refreshTokenStr = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            // Tìm refresh token trong database
            RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

            // Verify token chưa expired và chưa revoked
            refreshToken = refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();
            UserPrincipal userPrincipal = UserPrincipal.create(user);

            // Tạo access token mới
            String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

            // Tạo refresh token mới và revoke token cũ
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

            // Set access token vào cookie
            tokenCookieUtil.setTokenCookies(httpResponse, newAccessToken, newRefreshToken.getToken());

            return JwtAuthenticationResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Could not refresh token: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String refreshTokenStr = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.REFRESH_TOKEN_COOKIE_NAME);

            // Revoke refresh token trong database
            refreshTokenService.revokeToken(refreshTokenStr);

            // Thêm access token vào blacklist
            String accessToken = tokenCookieUtil.getCookieValue(httpRequest, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);
            if (accessToken != null && !accessToken.isEmpty()) {
                tokenBlacklistServiceImpl.blacklistToken(accessToken);
            }

            // Xoá cookie access token và refresh token
            tokenCookieUtil.clearCookies(httpResponse);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new RuntimeException("Error orcurred");
        }
    }

    @Override
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserResponse getCurrentUser(UserPrincipal currentUser) {
        User user = userRepository.findUserWithRolesById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toUserResponse(user);
    }

    @Override
    public void forgotPassword(String email) {
        // 1. Check user tồn tại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Sinh OTP ngẫu nhiên (Ví dụ 6 số)
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // 3. Lưu OTP vào Redis (Sống 5 phút)
        redisTemplate.opsForValue().set(
                RESET_PASS_PREFIX + email,
                otp,
                5,
                TimeUnit.MINUTES
        );

        // 4. Tạo nội dung email
        EmailMessage message = EmailMessage.builder()
                .to(email)
                .subject("Reset Password - OJ Core")
                .content("<h1>Mã xác thực của bạn là: " + otp + "</h1>" +
                        "<p>Mã này sẽ hết hạn sau 5 phút.</p>")
                .build();

        // 5. Bắn sang RabbitMQ (Async)
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_QUEUE, message);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) throws BadRequestException {
        // 1. Lấy OTP từ Redis
        String cachedOtp = redisTemplate.opsForValue().get(RESET_PASS_PREFIX + request.getEmail());

        // 2. Validate
        if (cachedOtp == null) {
            throw new BadRequestException("OTP đã hết hạn hoặc không tồn tại.");
        }
        if (!cachedOtp.equals(request.getOtp())) {
            throw new BadRequestException("OTP không chính xác.");
        }

        // 3. Đổi mật khẩu
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. Xóa OTP khỏi Redis (Tránh dùng lại)
        redisTemplate.delete(RESET_PASS_PREFIX + request.getEmail());
    }

    @Override
    public void sendVerificationEmail(UUID userId) {
        log.info("Starting sendVerificationEmail for userId: {}", userId);

        // 1. Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("Found user: {}, email: {}", user.getUsername(), user.getEmail());

        // 2. Check user đã verify chưa
        if (user.getEmailVerified()) {
            log.warn("Email already verified for user: {}", user.getEmail());
            throw new ResourceAlreadyExistsException("Email đã được xác thực");
        }

        // 3. Sinh token ngẫu nhiên (UUID)
        String token = UUID.randomUUID().toString();
        log.info("Generated verification token: {}", token);

        // 4. Lưu token vào Redis với userId (Sống 24 giờ)
        redisTemplate.opsForValue().set(
                VERIFY_EMAIL_PREFIX + token,
                userId.toString(),
                24,
                TimeUnit.HOURS
        );
        log.info("Saved token to Redis with key: {}", VERIFY_EMAIL_PREFIX + token);

        // 5. Tạo link xác thực
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        // 6. Tạo nội dung email
        EmailMessage message = EmailMessage.builder()
                .to(user.getEmail())
                .subject("Xác thực Email - OJ Core")
                .content("<h1>Chào " + user.getFullName() + ",</h1>" +
                        "<p>Cảm ơn bạn đã đăng ký tài khoản tại OJ Core!</p>" +
                        "<p>Vui lòng click vào link bên dưới để xác thực email của bạn:</p>" +
                        "<p><a href=\"" + verificationLink + "\" style=\"background-color: #4CAF50; color: white; padding: 14px 20px; text-align: center; text-decoration: none; display: inline-block; border-radius: 4px;\">Xác thực Email</a></p>" +
                        "<p>Hoặc copy link này vào trình duyệt:</p>" +
                        "<p>" + verificationLink + "</p>" +
                        "<p>Link này sẽ hết hạn sau 24 giờ.</p>" +
                        "<p>Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.</p>")
                .build();

        log.info("Email message created for: {}", user.getEmail());

        // 7. Bắn sang RabbitMQ (Async)
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_QUEUE, message);
            log.info("Email message sent to RabbitMQ queue: {}", RabbitMQConfig.EMAIL_QUEUE);
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void verifyEmail(String token) throws BadRequestException {
        // 1. Lấy userId từ Redis
        String userIdStr = redisTemplate.opsForValue().get(VERIFY_EMAIL_PREFIX + token);

        // 2. Validate token
        if (userIdStr == null) {
            throw new BadRequestException("Link xác thực đã hết hạn hoặc không hợp lệ.");
        }

        UUID userId = UUID.fromString(userIdStr);

        // 3. Tìm user và cập nhật trạng thái
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check nếu đã verify rồi
        if (user.getEmailVerified()) {
            throw new BadRequestException("Email đã được xác thực trước đó.");
        }

        // Cập nhật email verified và active account
        user.setEmailVerified(true);
        user.setStatus(EStatus.ACTIVE);
        userRepository.save(user);

        // 4. Xóa token khỏi Redis (Tránh dùng lại)
        redisTemplate.delete(VERIFY_EMAIL_PREFIX + token);
    }
}

