package com.kma.ojcore.service.impl;

import com.kma.ojcore.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service quản lý Token Blacklist trong Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpirationMs;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Thêm token vào blacklist
     * @param token JWT token cần blacklist
     */
    @Override
    public void blacklistToken(String token) {
        try {
            // Set TTL cho token bằng thời gian tồn tại của token
            long expirationMs = jwtAccessExpirationMs;

            String key = BLACKLIST_PREFIX + token;
            // Lưu vào Redis với TTL bằng thời gian còn lại của token
            redisTemplate.opsForValue().set(key, "blacklisted", expirationMs, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist with TTL: {} ms", expirationMs);
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    /**
     * Kiểm tra token có trong blacklist không
     * @param token JWT token cần kiểm tra
     * @return true nếu token bị blacklist, false nếu không
     */
    @Override
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            // Trong trường hợp lỗi Redis, cho phép request đi qua để tránh block toàn bộ hệ thống
            return false;
        }
    }

}

