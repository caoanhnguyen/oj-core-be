package com.kma.ojcore.service.impl;

import com.kma.ojcore.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpirationMs;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    @Override
    public void blacklistToken(String token) {
        try {
            long expirationMs = jwtAccessExpirationMs;
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", expirationMs, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist with TTL: {} ms", expirationMs);
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            return false;
        }
    }
}