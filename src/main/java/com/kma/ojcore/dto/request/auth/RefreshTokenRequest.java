package com.kma.ojcore.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    String refreshToken;
}

