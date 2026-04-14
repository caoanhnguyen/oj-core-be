package com.kma.ojcore.security;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kma.ojcore.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kma.ojcore.enums.EStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetails implementation for both normal and OAuth2 authentication
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal implements UserDetails, OAuth2User, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    UUID id;
    String username;
    String fullName;
    String email;
    String avatarUrl;
    @JsonIgnore
    String password;
    EStatus status;
    boolean accountNonLocked;
    Integer tokenVersion;

    @JsonDeserialize(contentAs = SimpleGrantedAuthority.class)
    Collection<? extends GrantedAuthority> authorities;

    Map<String, Object> attributes; // Dùng để lưu trữ thông tin từ OAuth2 provider

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getPassword(),
                user.getStatus(),
                user.getAccountNonLocked(),
                user.getTokenVersion(),
                authorities,
                null
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == EStatus.ACTIVE;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // OAuth2User methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean hasRole(String roleName) {
        if (authorities == null) return false;
        return authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(roleName));
    }
}

