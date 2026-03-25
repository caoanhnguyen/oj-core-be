package com.kma.ojcore.controller.users;

import com.kma.ojcore.dto.request.users.BulkUpdateLockSdi;
import com.kma.ojcore.dto.request.users.UpdateRoleSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.users.UserBasicSdo;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.enums.RoleName;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("")
    public ApiResponse<?> getAllUsersForAdmin(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Boolean isLocked,
                                              @RequestParam(required = false) RoleName role,
                                              @RequestParam(required = false, defaultValue = "0") int page,
                                              @RequestParam(required = false, defaultValue = "20") int size,
                                              @SortDefault(sort = "username", direction = Sort.Direction.DESC) Sort sort) {

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserBasicSdo> usersPage = userService.getAllUsersForAdmin(keyword, isLocked, role, pageable);
        return ApiResponse.<Page<UserBasicSdo>>builder()
                .status(200)
                .message("Get users for admin successfully!")
                .data(usersPage)
                .build();

    }

    @GetMapping("/{username}")
    public ApiResponse<?> getUserByUsername(@PathVariable String username) {
        UserDetailsSdo user = userService.getUserProfileByUsername(username, true);
        return ApiResponse.<UserDetailsSdo>builder()
                .status(200)
                .message("Get user profile successfully!")
                .data(user)
                .build();
    }

    @PatchMapping("/bulk-toggle-lock")
    public ApiResponse<?> bulkToggleLock(@Valid @RequestBody BulkUpdateLockSdi request,
                                         @AuthenticationPrincipal UserPrincipal adminUser) {
        userService.bulkToggleUserLock(adminUser.getId(), request.getUserIds(), request.getAccountNonLocked());
        return ApiResponse.builder()
                .status(200)
                .message("Bulk toggle lock/unlock users successfully!")
                .build();
    }

    @PatchMapping("/{id}/roles")
    public ApiResponse<?> updateUserRoles(@PathVariable UUID id,
                                          @RequestBody UpdateRoleSdi request,
                                          @AuthenticationPrincipal UserPrincipal adminUser) {

        userService.updateUserRoles(adminUser.getId(), id, request.getRoles());
        return ApiResponse.<UserDetailsSdo>builder()
                .status(200)
                .message("Update user roles successfully!")
                .build();
    }


}
