package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.response.UserResponse;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.mapper.UserMapper;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * User Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findUserWithRolesById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return userMapper.toUserResponse(user);
    }

}

