package com.kma.ojcore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    // Tiền tố lưu trên Redis, ví dụ: "SUBMIT_CODE:"
    String keyPrefix() default "RATE_LIMIT:";
    
    // Thời gian timeout
    int timeout() default 5;
    
    // Đơn vị thời gian
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    // Lời nhắn lỗi trả về
    String errorMessage() default "Too many requests. Please try again later.";
}
