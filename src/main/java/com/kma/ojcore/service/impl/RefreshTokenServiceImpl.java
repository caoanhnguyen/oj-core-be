package com.kma.ojcore.service.impl;

import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {

        refreshTokenRepository.revokeAllUserTokens(userId);

        String tokenString = jwtTokenProvider.generateRefreshToken(userId);
        Instant expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        User userProxy = userRepository.getReferenceById(userId);

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
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
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