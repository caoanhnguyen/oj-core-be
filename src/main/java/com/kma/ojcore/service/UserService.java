package com.kma.ojcore.service;

import com.kma.ojcore.dto.request.users.UpdateUserSdi;
import com.kma.ojcore.dto.response.users.UserBasicSdo;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    UserDetailsSdo getUserProfileById(UUID userId, boolean isMine);

    UserDetailsSdo getUserProfileByUsername(String username, boolean isMine);

    UserDetailsSdo updateUserProfile(UUID userId, UpdateUserSdi request);

    String updateAvatar(UUID userId, MultipartFile avatarFile);

    // ADMIN
    Page<UserBasicSdo> getAllUsersForAdmin(String keyword, Boolean isLocked, RoleName role, Pageable pageable);

    void bulkToggleUserLock(UUID adminId, List<UUID> userTargetIds, boolean accountNonLocked);

    void updateUserRoles(UUID adminId, UUID targetUserId, Set<RoleName> roleNames);

}
