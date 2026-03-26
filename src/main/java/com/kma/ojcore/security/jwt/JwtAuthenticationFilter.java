package com.kma.ojcore.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kma.ojcore.dto.response.common.ErrorResponse;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.security.CustomUserDetailsService;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.impl.TokenBlacklistServiceImpl;
import com.kma.ojcore.utils.TokenCookieUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenCookieUtil tokenCookieUtil;
    private final TokenBlacklistServiceImpl tokenBlacklistServiceImpl;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    @Value("${app.api.prefix}")
    private String apiPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.startsWith("/login/") ||
                path.startsWith(apiPrefix + "/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = tokenCookieUtil.getCookieValue(request, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);

            if (StringUtils.hasText(jwt)) {
                if (tokenBlacklistServiceImpl.isBlacklisted(jwt)) {
                    log.warn("Token is blacklisted");
                    sendErrorResponse(response, ErrorCode.TOKEN_INVALID, "Token is blacklisted or revoked.", request.getServletPath());
                    return;
                }

                Claims claims = tokenProvider.getAccessTokenClaims(jwt);
                UUID userId = UUID.fromString(claims.getSubject());
                Integer tokenVersion = claims.get("token_version", Integer.class);

                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UserPrincipal userPrincipal = (UserPrincipal) userDetails;

                if (!userPrincipal.isAccountNonLocked()) {
                    log.warn("User {} is locked but attempting access", userId);
                    sendErrorResponse(response, ErrorCode.ACCOUNT_LOCKED, null, request.getServletPath());
                    return;
                }

                if (tokenVersion == null || !tokenVersion.equals(userPrincipal.getTokenVersion())) {
                    log.warn("Token version mismatch for user {}. Forcing re-login.", userId);
                    sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED, "Token version expired. Please log in again.", request.getServletPath());
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("Expired JWT token");
            sendErrorResponse(response, ErrorCode.TOKEN_EXPIRED, null, request.getServletPath());
            return;
        } catch (io.jsonwebtoken.SignatureException | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            sendErrorResponse(response, ErrorCode.TOKEN_INVALID, null, request.getServletPath());
            return;
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            sendErrorResponse(response, ErrorCode.UNAUTHENTICATED, "Authentication error", request.getServletPath());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String customMessage, String path) throws IOException {
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType("application/json; charset=UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(new Date())
                .status(errorCode.getStatusCode().value())
                .errorCode(errorCode.getCode())
                .error(errorCode.getStatusCode().getReasonPhrase())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .path(path)
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}