package com.kma.ojcore.service.impl;

import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.repository.RefreshTokenRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.jwt.JwtTokenProvider;
import com.kma.ojcore.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service quản lý RefreshToken trong database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(UUID userId) { // Chỉ nhận UUID

        // 1. Thu hồi toàn bộ token cũ bằng 1 câu lệnh UPDATE
        refreshTokenRepository.revokeAllUserTokens(userId);

        // 2. Tạo token string và tính hạn sử dụng
        String tokenString = jwtTokenProvider.generateRefreshToken(userId); // Nếu hàm cũ của bro cần UUID
        Instant expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        // Lấy User ảo (Không chọc xuống DB)
        User userProxy = userRepository.getReferenceById(userId);

        // 4. Build và lưu Token mới
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(userProxy)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now()) || token.getRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn hoặc đã bị thu hồi. Vui lòng đăng nhập lại!");
        }
        return token;
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}

