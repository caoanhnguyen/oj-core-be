package com.kma.ojcore.aspect;

import com.kma.ojcore.annotation.RateLimit;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final Environment environment;

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu API cho phép nặc danh (không có UserPrincipal), có thể mở rộng chặn theo IP (bằng cách lấy HttpServletRequest).
        // Tuy nhiên submitCode bắt buộc đăng nhập nên logic này lấy trực tiếp từ Principal.
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
             return joinPoint.proceed();
        }

        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        String userId = userPrincipal.getId().toString();

        // 1. Phân giải biến môi trường nếu gán template ${...} từ application.yml
        String mappedPrefix = rateLimit.keyPrefix();
        if (mappedPrefix.startsWith("${") && mappedPrefix.endsWith("}")) {
            mappedPrefix = environment.resolvePlaceholders(mappedPrefix);
        }

        // Tạo Redis Key. VD: SUBMIT_CODE:123-456-789
        String redisKey = mappedPrefix + userId;

        // setIfAbsent tương đương với lệnh SETNX trong Redis.
        // Trả về TRUE nếu chưa có key (tạo thành công), FALSE nếu key đã tồn tại (đang bị spam).
        Boolean isAllowed = redisTemplate.opsForValue().setIfAbsent(redisKey, "LOCKED", rateLimit.timeout(), rateLimit.timeUnit());

        if (Boolean.FALSE.equals(isAllowed)) {
            log.warn("Rate limit triggered for User [{}]. Key: {}", userId, rateLimit.keyPrefix());
            // Quăng Exception 429 TOO_MANY_REQUESTS
            throw new BusinessException(ErrorCode.SUBMISSION_LIMIT_EXCEEDED, rateLimit.errorMessage());
        }

        // Nếu hợp lệ, cho phép qua chốt đoạn code gốc (submitCode)
        return joinPoint.proceed();
    }
}
