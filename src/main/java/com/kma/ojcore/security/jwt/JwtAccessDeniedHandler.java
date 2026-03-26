package com.kma.ojcore.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.dto.response.common.ErrorResponse;
import com.kma.ojcore.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.error("Access Denied error: {}", accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .errorCode(ErrorCode.UNAUTHORIZED.getCode())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ErrorCode.UNAUTHORIZED.getMessage())
                .path(request.getServletPath())
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}