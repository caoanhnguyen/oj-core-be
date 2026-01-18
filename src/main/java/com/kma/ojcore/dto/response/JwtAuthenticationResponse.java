package com.kma.ojcore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO chứa thông tin user sau khi đăng nhập
 * Note: accessToken sẽ được set vào cookie, không trả trong response body
 * refreshToken sẽ được lưu vào database, không trả về client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class JwtAuthenticationResponse {

    UUID userId;
    String username;
    String email;
    String fullName;
}


