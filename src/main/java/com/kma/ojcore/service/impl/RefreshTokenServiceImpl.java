package com.kma.ojcore.service.impl;

import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.repository.RefreshTokenRepository;
import com.kma.ojcore.security.jwt.JwtTokenProvider;
import com.kma.ojcore.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service quản lý RefreshToken trong database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke các token cũ của user
        refreshTokenRepository.findByUserAndRevokedFalse(user).ifPresent(oldToken -> {
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);
        });

        // Tạo token mới
        String tokenString = jwtTokenProvider.generateRefreshToken(user.getId());
        Instant expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
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

