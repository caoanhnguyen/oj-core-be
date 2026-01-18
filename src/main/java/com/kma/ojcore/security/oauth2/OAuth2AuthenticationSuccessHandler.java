package com.kma.ojcore.security.oauth2;

import com.kma.ojcore.entity.RefreshToken;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.security.jwt.JwtTokenProvider;
import com.kma.ojcore.service.RefreshTokenService;
import com.kma.ojcore.utils.TokenCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * OAuth2 Authentication Success Handler
 * Lưu refreshToken vào DB và set accessToken vào cookie
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final TokenCookieUtil tokenCookieUtil;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${jwt.access-expiration}")
    private long jwtAccessExpirationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        // Tạo access token
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // Lưu refresh token vào database
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findUserWithRolesById(Objects.requireNonNull(userPrincipal).getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Set access token vào cookie
        tokenCookieUtil.setTokenCookies(response, accessToken, refreshToken.getToken());

        // Redirect về frontend
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("success", "true")
                .build().toUriString();
    }
}

