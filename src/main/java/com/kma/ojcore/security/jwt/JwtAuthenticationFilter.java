package com.kma.ojcore.security.jwt;

import com.kma.ojcore.security.CustomUserDetailsService;
import com.kma.ojcore.service.impl.TokenBlacklistServiceImpl;
import com.kma.ojcore.utils.TokenCookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter: Lấy JWT từ cookie, xác thực và thiết lập thông tin người dùng trong SecurityContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenCookieUtil tokenCookieUtil;
    private final TokenBlacklistServiceImpl tokenBlacklistServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bỏ qua các path public
        if (path.startsWith("/oauth2/") ||
            path.startsWith("/login/oauth2/") ||
            path.startsWith("/login/") ||
            path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = tokenCookieUtil.getCookieValue(request, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);

            if (StringUtils.hasText(jwt)) {
                // Kiểm tra token có bị blacklist không
                if (tokenBlacklistServiceImpl.isBlacklisted(jwt)) {
                    log.warn("Token is blacklisted");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token is blacklisted\",\"path\":\"" + request.getServletPath() + "\"}");
                    return;
                }

                // Parse token để bắt lỗi hết hạn
                try {
                    tokenProvider.parseAccessToken(jwt); // Nếu hết hạn sẽ ném ExpiredJwtException
                } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                    log.warn("Expired JWT token");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token expired\",\"path\":\"" + request.getServletPath() + "\"}");
                    return;
                }

                // Validate token
                try {
                    if (tokenProvider.validateAccessToken(jwt)) {
                        UUID userId = tokenProvider.getUserIdFromAccessToken(jwt);
                        UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid token\",\"path\":\"" + request.getServletPath() + "\"}");
                        return;
                    }
                } catch (io.jsonwebtoken.SignatureException | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException ex) {
                    log.warn("Invalid JWT token: {}", ex.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid token\",\"path\":\"" + request.getServletPath() + "\"}");
                    return;
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication error\",\"path\":\"" + request.getServletPath() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
