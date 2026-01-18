package com.kma.ojcore.security.oauth2.user;

import java.util.Map;

/**
 * GitHub OAuth2 User Info
 */
public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        String name = (String) attributes.get("name");
        // Nếu không có name, dùng login (username)
        if (name == null || name.isEmpty()) {
            name = (String) attributes.get("login");
        }
        return name;
    }

    @Override
    public String getEmail() {
        // GitHub có thể không trả về email nếu user set private
        // Trả về null nếu không có email
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}

