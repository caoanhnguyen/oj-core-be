package com.kma.ojcore.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ApiResponse<T> {
    final int status;
    final String message;
    
    @Builder.Default
    LocalDateTime serverTime = LocalDateTime.now();
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    T data;
}

