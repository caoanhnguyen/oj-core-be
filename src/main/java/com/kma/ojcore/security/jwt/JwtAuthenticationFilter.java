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
    private final TokenCookieUtil tokenCookieUtil;
    private final TokenBlacklistServiceImpl tokenBlacklistServiceImpl;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bỏ qua các path public
        if (path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.startsWith("/login/") ||
                path.startsWith("${app.api.prefix}/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = tokenCookieUtil.getCookieValue(request, tokenCookieUtil.ACCESS_TOKEN_COOKIE_NAME);

            if (StringUtils.hasText(jwt)) {
                // 1. Check Blacklist (Đăng xuất)
                if (tokenBlacklistServiceImpl.isBlacklisted(jwt)) {
                    log.warn("Token is blacklisted");
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted", request.getServletPath());
                    return;
                }

                // 2. Lấy ID từ Token
                Claims claims = tokenProvider.getAccessTokenClaims(jwt);
                UUID userId = UUID.fromString(claims.getSubject());

                Integer tokenVersion = claims.get("token_version", Integer.class);

                // 3. Hỏi Redis (thông qua Service) lấy thông tin mới nhất
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UserPrincipal userPrincipal = (UserPrincipal) userDetails;

                // 4.1 Kiểm tra tài khoản có bị khóa hay không, nếu có thì chặn luôn
                if (!userPrincipal.isAccountNonLocked()) {
                    log.warn("User {} bị khóa nhưng cố tình truy cập", userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Tài khoản của bạn đã bị khóa!", request.getServletPath());
                    return;
                }

                // 4.2 Kiểm tra token version để phát hiện token cũ (bị thu hồi)
                if (tokenVersion == null || !tokenVersion.equals(userPrincipal.getTokenVersion())) {
                    log.warn("Token version mismatch cho user {}. Bắt buộc đăng nhập lại.", userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Phiên bản đăng nhập đã hết hạn, vui lòng đăng nhập lại!", request.getServletPath());
                    return;
                }

                // 5. Cấp quyền đi tiếp
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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
        response.setContentType("application/json; charset=UTF-8"); // Thêm UTF-8 để hiển thị tiếng Việt chuẩn
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"path\":\"" + path + "\"}");
    }
}