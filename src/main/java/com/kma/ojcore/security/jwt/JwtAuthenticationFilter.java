package com.kma.ojcore.security.jwt;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter: Lấy JWT từ cookie, xác thực và thiết lập thông tin người dùng trong SecurityContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
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
            path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = tokenCookieUtil.getCookieValue(request, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);

            if (StringUtils.hasText(jwt)) {
                if (tokenBlacklistServiceImpl.isBlacklisted(jwt)) {
                    log.warn("Token is blacklisted");
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted", request.getServletPath());
                    return;
                }

                // GIẢI MÃ ĐÚNG 1 LẦN DUY NHẤT LẤY RA TOÀN BỘ DATA
                Claims claims = tokenProvider.getAccessTokenClaims(jwt);

                // Moi móc dữ liệu từ trong Token ra (KHÔNG CHỌC DB NỮA)
                UUID userId = UUID.fromString(claims.getSubject());
                String username = claims.get("username", String.class);
                String email = claims.get("email", String.class);
                String fullName = claims.get("fullName", String.class);

                // Lấy mảng roles mà bro đã ném vào lúc generate token
                List<String> roles = claims.get("roles", List.class);
                if (roles == null) roles = new ArrayList<>();

                // Chuyển mảng chuỗi thành mảng Quyền (GrantedAuthority)
                List<GrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Tự tay nặn ra đối tượng UserPrincipal ngay trên RAM (Các trường entity để null)
                UserPrincipal principal = new UserPrincipal(
                        userId, username, fullName, email, null, null, null, authorities, null
                );

                // Gắn mác VIP (Authentication) cho thanh niên này
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("Expired JWT token");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired", request.getServletPath());
            return;
        } catch (io.jsonwebtoken.SignatureException | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token", request.getServletPath());
            return;
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication error", request.getServletPath());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"path\":\"" + path + "\"}");
    }
}
