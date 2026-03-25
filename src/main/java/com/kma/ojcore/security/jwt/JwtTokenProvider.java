package com.kma.ojcore.security.jwt;

import com.kma.ojcore.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * JWT Token Provider: Chịu trách nhiệm tạo, giải mã và xác thực JWT cho cả Access Token và Refresh Token
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    public long getRefreshTokenExpirationMs() {
        return jwtRefreshExpirationMs;
    }

    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(Objects.requireNonNull(userPrincipal));
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .id(UUID.randomUUID().toString()) // JWT ID (jti) để nhận biết duy nhất từng token, dùng cho blacklist
                .claim("username", userPrincipal.getUsername())
                .claim("token_version", userPrincipal.getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtAccessExpirationMs))
                .signWith(getSigningKey(accessSecret))
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(getSigningKey(refreshSecret))
                .compact();
    }

    public UUID getUserIdFromAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(accessSecret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    // Thêm hàm này vào JwtTokenProvider
    public Claims getAccessTokenClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey(accessSecret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateAccessToken(String authToken) {
        return validateToken(authToken, accessSecret);
    }

    public boolean validateRefreshToken(String authToken) {
        return validateToken(authToken, refreshSecret);
    }

    private boolean validateToken(String authToken, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    public void parseAccessToken(String token) {
        Jwts.parser()
                .verifyWith(getSigningKey(accessSecret))
                .build()
                .parseSignedClaims(token);
    }
}
