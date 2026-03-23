package com.kma.ojcore.controller.users;

import com.kma.ojcore.dto.request.users.UpdateUserSdi;
import com.kma.ojcore.dto.response.auth.UserResponse;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.AuthService;
import com.kma.ojcore.service.UserService;
import com.kma.ojcore.dto.response.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;


//    @GetMapping("/{id}")
//    public ApiResponse<?> getUserById(@PathVariable UUID id) {
//        UserDetailsSdo userProfile = userService.getUserProfileById(id, false);
//        return ApiResponse.<UserDetailsSdo>builder()
//                .status(200)
//                .message("Get user profile successfully!")
//                .data(userProfile)
//                .build();
//    }

    @GetMapping("/{username}")
    public ApiResponse<?> getUserByUsername(@PathVariable String username) {
        UserDetailsSdo userProfile = userService.getUserProfileByUsername(username, false);
        return ApiResponse.<UserDetailsSdo>builder()
                .status(200)
                .message("Get user profile successfully!")
                .data(userProfile)
                .build();
    }

//    @GetMapping("/me")
//    @PreAuthorize("isAuthenticated()")
//    public ApiResponse<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
//        UserDetailsSdo userProfile = userService.getUserProfileById(currentUser.getId(), true);
//        return ApiResponse.<UserDetailsSdo>builder()
//                .status(200)
//                .message("Get current user successfully!")
//                .data(userProfile)
//                .build();
//    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse userResponse = authService.getCurrentUser(currentUser);
        return ApiResponse.<UserResponse>builder()
                .status(200)
                .message("Get current user successful")
                .data(userResponse)
                .build();
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> updateCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser,
                                            @RequestBody UpdateUserSdi request) {
        UserDetailsSdo updatedProfile = userService.updateUserProfile(currentUser.getId(), request);
        return ApiResponse.<UserDetailsSdo>builder()
                .status(200)
                .message("Update user profile successfully!")
                .data(updatedProfile)
                .build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> uploadAvatar(@AuthenticationPrincipal UserPrincipal currentUser,
                                       @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống!");
        }

        String newAvatarUrl = userService.updateAvatar(currentUser.getId(), file);

        return ApiResponse.<String>builder()
                .status(200)
                .message("Cập nhật ảnh đại diện thành công!")
                .data(newAvatarUrl)
                .build();
    }
}

