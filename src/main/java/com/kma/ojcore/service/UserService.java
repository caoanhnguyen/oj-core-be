package com.kma.ojcore.service;

import com.kma.ojcore.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse getUserById(UUID id);

    UserResponse getUserByUsername(String username);


}
