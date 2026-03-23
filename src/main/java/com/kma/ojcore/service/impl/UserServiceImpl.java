package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.users.UpdateUserSdi;
import com.kma.ojcore.dto.response.users.UserBasicSdo;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.RoleName;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ResourceNotFoundException;
import com.kma.ojcore.mapper.UserMapper;
import com.kma.ojcore.repository.RoleRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.ImageStorageService;
import com.kma.ojcore.service.UserService;
import com.kma.ojcore.utils.EscapeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final ImageStorageService imageStorageService;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public String updateAvatar(UUID userId, MultipartFile avatarFile) {
        // 1. Tìm user trong DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user!"));

        String oldAvatarUrl = user.getAvatarUrl();

        // 2. Upload file mới vào folder "avatar"
        String newAvatarUrl = imageStorageService.uploadImage(avatarFile, "avatar");

        // 3. Dọn rác: Xóa ảnh cũ an toàn (Chặn xóa nhầm ảnh mặc định default.png)
        if (oldAvatarUrl != null && !oldAvatarUrl.contains("default.png")) {
            imageStorageService.deleteImageByUrl(oldAvatarUrl);
        }

        // 4. Lưu URL mới vào Database
        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        return newAvatarUrl;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetailsSdo getUserProfileById(UUID userId, boolean isMine) {
        User user = userRepository.findByUserIdAndStatusIsActive(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user!"));

        return userMapper.toUserDetailsSdo(user, isMine);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetailsSdo getUserProfileByUsername(String username, boolean isMine) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user!"));

        return userMapper.toUserDetailsSdo(user, isMine);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UserDetailsSdo updateUserProfile(UUID userId, UpdateUserSdi request) {
        User user = userRepository.findByUserIdAndStatusIsActive(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user!"));

        userMapper.UpdateUserFromUpdateSdi(request, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toUserDetailsSdo(updatedUser, true);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserBasicSdo> getAllUsersForAdmin(String keyword, Boolean isLocked, RoleName role, Pageable pageable) {

        String searchKeyword = EscapeHelper.escapeLike(keyword);

        // 1. Query phân trang
        Page<User> usersPage = userRepository.searchUsersForAdmin(searchKeyword, isLocked, role, pageable);

        if (usersPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. Lấy list userIds để query batch lấy roles
        List<UUID> userIds = usersPage.getContent().stream()
                .map(User::getId)
                .toList();

        // 3. Query batch lấy user + roles
        List<User> usersWithRoles = userRepository.findUsersWithRolesByIds(userIds);

        // 4. Chuyển thành Map<UUID, User> để dễ lấy role khi map sang SDO
        Map<UUID, User> userMap = usersWithRoles.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. Map sang UserBasicSdo, lấy role từ userMap, trả về Page<UserBasicSdo>
        return usersPage.map(user -> {
            User userWithRoles = userMap.get(user.getId());
            return UserBasicSdo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .accountNonLocked(user.getAccountNonLocked())
                    .roles(userWithRoles != null ? userMapper.rolesToRoleNames(userWithRoles.getRoles()) : Set.of())
                    .createdDate(user.getCreatedDate())
                    .updatedDate(user.getUpdatedDate())
                    .build();
        });

    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void bulkToggleUserLock(UUID adminId, List<UUID> userTargetIds, boolean accountNonLocked) {
        if (userTargetIds.contains(adminId)) {
            throw new IllegalArgumentException("Không thể tự khóa/mở khóa tài khoản của mình!");
        }

        List<User> users = userRepository.findAllById(userTargetIds);
        if (users.size() != userTargetIds.size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều user không tồn tại!");
        }

        userRepository.bulkUpdateAccountNonLocked(accountNonLocked, userTargetIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void updateUserRoles(UUID adminId, UUID targetUserId, Set<RoleName> roleNames) {
        // 1. Kiểm tra không cho admin tự thay đổi role của mình
        if (adminId.equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể tự thay đổi role của mình!");
        }

        // 2. Lấy user mục tiêu, nếu không tồn tại thì ném ResourceNotFound
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user!"));


        // 3. Valid roleNames không được null
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("Role names không được null!");
        }

        // 4. Tìm các Entity Role từ DB dựa vào tên Frontend gửi lên
        Set<Role> newRoles = roleRepository.findByNameIn(roleNames);
        if (newRoles.isEmpty()) {
            throw new BusinessException("Danh sách quyền không hợp lệ!");
        }

        // 5. Cập nhật role cho user và lưu vào DB
        targetUser.getRoles().clear();
        targetUser.getRoles().addAll(newRoles);
        userRepository.save(targetUser);
    }
}

