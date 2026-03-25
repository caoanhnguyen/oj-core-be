package com.kma.ojcore.dto.response.auth;

import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO chứa thông tin user sau khi đăng nhập và accessToken + refreshToken để gán vào cookie trên Controller
 * Note: accessToken sẽ được set vào cookie, không trả trong response body
 * refreshToken sẽ được lưu vào database, không trả về client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class JwtAuthenticationResponse {

    String accessToken;
    String refreshToken;
    UserDetailsSdo user;
}


