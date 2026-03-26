package com.kma.ojcore.service.impl;

import com.kma.ojcore.dto.request.users.UpdateUserSdi;
import com.kma.ojcore.dto.response.users.HeatMapItemSdo;
import com.kma.ojcore.dto.response.users.UserBasicSdo;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.dto.response.users.UserHeatMapSdo;
import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import com.kma.ojcore.enums.RoleName;
import com.kma.ojcore.exception.BusinessException;
import com.kma.ojcore.exception.ErrorCode;
import com.kma.ojcore.mapper.UserMapper;
import com.kma.ojcore.repository.RoleRepository;
import com.kma.ojcore.repository.SubmissionRepository;
import com.kma.ojcore.repository.UserRepository;
import com.kma.ojcore.service.ImageStorageService;
import com.kma.ojcore.service.UserService;
import com.kma.ojcore.utils.EscapeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final ImageStorageService imageStorageService;
    private final SubmissionRepository submissionRepo;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public String updateAvatar(UUID userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = imageStorageService.uploadImage(avatarFile, "avatar");

        if (oldAvatarUrl != null && !oldAvatarUrl.contains("default.png")) {
            imageStorageService.deleteImageByUrl(oldAvatarUrl);
        }

        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        return newAvatarUrl;
    }

    @Override
    public UserHeatMapSdo getContributionHeatMap(UUID userId) {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        List<LocalDateTime> submissionDates = submissionRepo.findSubmissionDatesByUserIdAndStartDate(userId, oneYearAgo);

        Map<LocalDateTime, Long> dateCountMap = submissionDates.stream()
                .collect(Collectors.groupingBy(date -> date.toLocalDate().atStartOfDay(), Collectors.counting()));

        List<HeatMapItemSdo> heatmapItems = dateCountMap.entrySet().stream()
                .map(entry -> new HeatMapItemSdo(entry.getKey(), entry.getValue().intValue()))
                .toList();

        return UserHeatMapSdo.builder()
                .totalSubmissions(submissionDates.size())
                .heatmapItems(heatmapItems)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetailsSdo getUserProfileById(UUID userId, boolean isMine) {
        User user = userRepository.findByUserIdAndStatusIsActive(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserDetailsSdo(user, isMine);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetailsSdo getUserProfileByUsername(String username, boolean isMine) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserDetailsSdo(user, isMine);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public UserDetailsSdo updateUserProfile(UUID userId, UpdateUserSdi request) {
        User user = userRepository.findByUserIdAndStatusIsActive(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        userMapper.UpdateUserFromUpdateSdi(request, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toUserDetailsSdo(updatedUser, true);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserBasicSdo> getAllUsersForAdmin(String keyword, Boolean isLocked, RoleName role, Pageable pageable) {

        String searchKeyword = EscapeHelper.escapeLike(keyword);

        Page<User> usersPage = userRepository.searchUsersForAdmin(searchKeyword, isLocked, role, pageable);

        if (usersPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> userIds = usersPage.getContent().stream()
                .map(User::getId)
                .toList();

        List<User> usersWithRoles = userRepository.findUsersWithRolesByIds(userIds);

        Map<UUID, User> userMap = usersWithRoles.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

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
    @CacheEvict(value = "userDetails", allEntries = true)
    public void bulkToggleUserLock(UUID adminId, List<UUID> userTargetIds, boolean accountNonLocked) {
        if (userTargetIds.contains(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF);
        }

        List<User> users = userRepository.findAllById(userTargetIds);
        if (users.size() != userTargetIds.size()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        userRepository.bulkUpdateAccountNonLocked(accountNonLocked, userTargetIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    @CacheEvict(value = "userDetails", key = "#targetUserId")
    public void updateUserRoles(UUID adminId, UUID targetUserId, Set<RoleName> roleNames) {
        if (adminId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUEST_PARAMETER);
        }

        Set<Role> newRoles = roleRepository.findByNameIn(roleNames);
        if (newRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid roles.");
        }

        targetUser.getRoles().clear();
        targetUser.getRoles().addAll(newRoles);
        userRepository.save(targetUser);
    }
}