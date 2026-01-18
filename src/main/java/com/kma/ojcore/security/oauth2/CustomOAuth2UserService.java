package com.kma.ojcore.security.oauth2;

import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.Provider;
import com.kma.ojcore.repository.RoleRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.security.oauth2.user.OAuth2UserInfo;
import com.kma.ojcore.security.oauth2.user.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Custom OAuth2 User Service: xử lý đăng ký và cập nhật user từ OAuth2 providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (OAuth2AuthenticationException ex) {
            log.error("OAuth2 authentication error: {}", ex.getError(), ex);
            throw ex; // preserve error code and message
        } catch (IllegalArgumentException ex) {
            log.error("Invalid provider: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException("Invalid OAuth2 provider: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException("Internal error during OAuth2 authentication: " + ex.getMessage());
        }
    }

    /**
     * Xử lý OAuth2 user: đăng ký mới hoặc cập nhật user hiện có
     * @param userRequest
     * @param oAuth2User
     * @return
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("Processing OAuth2 user from provider: {}", registrationId);
        log.debug("OAuth2 user attributes: {}", oAuth2User.getAttributes());

        OAuth2UserInfo oAuth2UserInfo;
        try {
            oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                    registrationId,
                    oAuth2User.getAttributes()
            );
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider or invalid user info: " + ex.getMessage());
        }

        log.info("OAuth2 user info - ID: {}, Name: {}, Email: {}",
                oAuth2UserInfo.getId(),
                oAuth2UserInfo.getName(),
                oAuth2UserInfo.getEmail());

        Optional<User> userOptional;
        try {
            userOptional = userRepository.findByProviderAndProviderId(
                    Provider.valueOf(registrationId.toUpperCase()),
                    oAuth2UserInfo.getId()
            );
        } catch (IllegalArgumentException ex) {
            throw new OAuth2AuthenticationException("Invalid provider: " + registrationId);
        }

        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("Found existing user: {}", user.getUsername());
            user = updateExistingUser(user, oAuth2UserInfo);
            Set<Role> roles = roleRepository.getRoleByUserId(user.getId());
            user.setRoles(roles);
            log.info("Updated existing user: {}", user.getUsername());
        } else {
            // Nếu có email, check xem email đã được dùng chưa
            if (StringUtils.hasText(oAuth2UserInfo.getEmail())) {
                Optional<User> existingUserByEmail = userRepository.findByEmail(oAuth2UserInfo.getEmail());
                if (existingUserByEmail.isPresent()) {
                    User existingUser = existingUserByEmail.get();
                    log.error("Email already registered with provider: {}", existingUser.getProvider());
                    throw new OAuth2AuthenticationException(
                            "Email already registered with provider: " + existingUser.getProvider()
                    );
                }
            }

            log.info("Registering new user from {}", registrationId);
            user = registerNewUser(userRequest, oAuth2UserInfo);
            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.getUserRole());
            user.setRoles(roles);
            log.info("Registered new user: {}", user.getUsername());
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    /**
     * Đăng ký user mới từ thông tin OAuth2
     * @param userRequest
     * @param oAuth2UserInfo
     * @return
     */
    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // Dùng name (login) từ GitHub làm username, hoặc từ email nếu có
        String baseUsername;
        if (StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            baseUsername = oAuth2UserInfo.getEmail().split("@")[0];
        } else {
            // Dùng name (đã được set từ login trong GithubOAuth2UserInfo)
            baseUsername = oAuth2UserInfo.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        User user = User.builder()
                .username(registrationId.toLowerCase() + "_" + baseUsername)
                .email(oAuth2UserInfo.getEmail()) // Có thể null
                .fullName(oAuth2UserInfo.getName())
                .avatarUrl(oAuth2UserInfo.getImageUrl())
                .provider(Provider.valueOf(registrationId.toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .emailVerified(StringUtils.hasText(oAuth2UserInfo.getEmail()))
                .build();

        Role defaultRole = roleRepository.getUserRole();
        Set<Role> roles = Set.of(defaultRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    /**
     * Cập nhật thông tin user hiện có từ OAuth2
     * @param existingUser
     * @param oAuth2UserInfo
     * @return
     */
    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setFullName(oAuth2UserInfo.getName());
        existingUser.setAvatarUrl(oAuth2UserInfo.getImageUrl());
        // Update email nếu có (trường hợp user vừa public email)
        if (StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            existingUser.setEmail(oAuth2UserInfo.getEmail());
            existingUser.setEmailVerified(true);
        }
        return userRepository.save(existingUser);
    }
}
