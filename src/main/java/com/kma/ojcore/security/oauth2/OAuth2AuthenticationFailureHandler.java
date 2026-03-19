package com.kma.ojcore.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String errorMessage = "Đăng nhập thất bại. Vui lòng thử lại!";
        if (exception instanceof OAuth2AuthenticationException) {
            errorMessage = ((OAuth2AuthenticationException) exception).getError().getDescription();
        } else if (exception.getLocalizedMessage() != null) {
            errorMessage = exception.getLocalizedMessage();
        }

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorMessage)
                .build()
                .encode() // Bắt buộc encode để chuỗi tiếng Việt không bị lỗi font
                .toUriString();

        log.error("OAuth2 authentication failed: {}", errorMessage);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}