package com.kma.ojcore.config;

import com.kma.ojcore.security.jwt.JwtAccessDeniedHandler;
import com.kma.ojcore.security.jwt.JwtAuthenticationEntryPoint;
import com.kma.ojcore.security.jwt.JwtAuthenticationFilter;
import com.kma.ojcore.security.oauth2.CustomOAuth2UserService;
import com.kma.ojcore.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.kma.ojcore.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration for Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

        @Value("${app.api.prefix}")
        private String apiPrefix;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(request -> {
                                        var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                                        corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:5173",
                                                        "http://localhost:5174", "http://localhost:8088"));
                                        corsConfig.setAllowedMethods(
                                                        java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                                        corsConfig.setAllowedHeaders(java.util.List.of("*"));
                                        corsConfig.setAllowCredentials(true);
                                        return corsConfig;
                                }))
                                .csrf(AbstractHttpConfigurer::disable)
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                                .accessDeniedHandler(jwtAccessDeniedHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth

                                    .requestMatchers(apiPrefix + "/auth/**").permitAll()

                                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                                    // Public API
                                    .requestMatchers(HttpMethod.GET, apiPrefix + "/system/languages").permitAll()
                                    .requestMatchers(HttpMethod.GET, apiPrefix + "/topics").permitAll()
                                    .requestMatchers(HttpMethod.GET,
                                            apiPrefix + "/problems",
                                            apiPrefix + "/problems/*",
                                            apiPrefix + "/problems/slug/*",
                                            apiPrefix + "/problems/*/statistics").permitAll()
                                    .requestMatchers(HttpMethod.GET, apiPrefix + "/submissions", apiPrefix + "/submissions/statistics").permitAll()
                                    .requestMatchers(HttpMethod.GET, apiPrefix + "/files/view").permitAll()
                                    .requestMatchers(HttpMethod.GET, apiPrefix + "/rankings").permitAll()

                                    // Public Contest API
                                    .requestMatchers(HttpMethod.GET,
                                            apiPrefix + "/contests",
                                            apiPrefix + "/contests/*",
                                            apiPrefix + "/contests/*/leaderboard",
                                            apiPrefix + "/contests/*/participants").permitAll()

                                    // Fine-grained access control is handled via @PreAuthorize on each controller.

                                    // Bắt buộc đăng nhập cho các request còn lại
                                    .anyRequest().authenticated()
                                )
                                .oauth2Login(oauth2 -> oauth2
                                                // Custom OAuth2 user service to load user info
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                // Success and failure handlers
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
