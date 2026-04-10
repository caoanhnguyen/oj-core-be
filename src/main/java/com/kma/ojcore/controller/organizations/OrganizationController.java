package com.kma.ojcore.controller.organizations;

import com.kma.ojcore.dto.request.organizations.OrganizationCreateSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationJoinSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationReviewJoinSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateMemberRoleSdi;
import com.kma.ojcore.dto.request.organizations.OrganizationUpdateSdi;
import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationJoinRequestSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationMemberSdo;
import com.kma.ojcore.dto.response.organizations.OrganizationSdo;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.OrganizationService;
import com.kma.ojcore.enums.OrgApprovalStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Tạo organization mới. User hiện tại sẽ trở thành ORG_OWNER.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> createOrganization(@Valid @RequestBody OrganizationCreateSdi request,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {

        OrganizationSdo orgId = organizationService.createOrganization(request, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Organization created successfully.")
                .data(orgId)
                .build();
    }

    /**
     * Tìm kiếm organization (public/user). Chỉ trả về ACTIVE orgs.
     */
    @GetMapping
    public ApiResponse<?> searchActiveOrganizations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrgApprovalStatus approvalStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrganizationBasicSdo> result = organizationService.searchOrganizations(keyword, approvalStatus, false, pageable);
        
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organizations retrieved successfully.")
                .data(result)
                .build();
    }

    /**
     * Lấy danh sách organization mà user hiện tại là member (ACTIVE).
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getMyOrganizations(@AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<OrganizationBasicSdo> organizations = organizationService.getMyOrganizations(userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organizations retrieved successfully.")
                .data(organizations)
                .build();
    }

    /**
     * Nộp đơn xin gia nhập
     */
    @PostMapping("/{orgId}/join")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> joinOrganization(@PathVariable UUID orgId,
                                           @Valid @RequestBody OrganizationJoinSdi request,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        organizationService.requestJoinOrganization(orgId, request, userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Join request submitted successfully.")
                .build();
    }

    /**
     * Hủy đơn xin gia nhập (nếu đang ở trạng thái PENDING)
     */
    @DeleteMapping("/{orgId}/join")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> cancelJoinRequest(@PathVariable UUID orgId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        organizationService.cancelJoinRequest(orgId, userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Join request cancelled successfully.")
                .build();
    }

    /**
     * Xem danh sách các đơn đang gửi của chính mình
     */
    @GetMapping("/join-requests/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getMyJoinRequests(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<OrganizationJoinRequestSdo> requests = organizationService.getMyJoinRequests(userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Join requests retrieved successfully.")
                .data(requests)
                .build();
    }

    /**
     * Lấy danh sách các đơn xin gia nhập chờ duyệt của 1 organization.
     */
    @GetMapping("/{orgId}/join-requests")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> getJoinRequests(@PathVariable UUID orgId,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<OrganizationJoinRequestSdo> requests = organizationService.getJoinRequests(orgId, userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Pending join requests retrieved successfully.")
                .data(requests)
                .build();
    }

    /**
     * Duyệt / Từ chối (bulk) đơn xin gia nhập.
     */
    @PostMapping("/{orgId}/join-requests/review")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> reviewJoinRequests(@PathVariable UUID orgId,
                                             @Valid @RequestBody OrganizationReviewJoinSdi request,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        organizationService.reviewJoinRequests(orgId, request, userPrincipal.getId());
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Join requests reviewed successfully.")
                .build();
    }

    /**
     * Lấy danh sách member của 1 organization.
     * Chỉ ORG_OWNER / ORG_ADMIN (hoặc Site Staff) mới xem được.
     */
    @GetMapping("/{orgId}/members")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    // TODO: sửa lại có tìm kiếm, filter, sort + phân trang
    public ApiResponse<?> getMembers(@PathVariable UUID orgId,
                                     @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<OrganizationMemberSdo> members =
                organizationService.getMembers(orgId, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization members retrieved successfully.")
                .data(members)
                .build();
    }

    /**
     * Cập nhật role của member trong organization.
     */
    @PatchMapping("/{orgId}/members/{memberId}/role")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> updateMemberRole(@PathVariable UUID orgId,
                                           @PathVariable UUID memberId,
                                           @Valid @RequestBody OrganizationUpdateMemberRoleSdi request,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {

        organizationService.updateMemberRole(orgId, memberId, request, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization member role updated successfully.")
                .build();
    }

    /**
     * Xóa (soft delete) member khỏi organization.
     */
    @DeleteMapping("/{orgId}/members/{memberId}")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> removeMember(@PathVariable UUID orgId,
                                       @PathVariable UUID memberId,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {

        organizationService.removeMember(orgId, memberId, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization member removed successfully.")
                .build();
    }

    /**
     * Cập nhật thông tin profile của organization.
     */
    @PatchMapping("/{orgId}")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> updateOrganizationProfile(@PathVariable UUID orgId,
                                                    @Valid @RequestBody OrganizationUpdateSdi request,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {

        organizationService.updateOrganizationProfile(orgId, request, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization profile updated successfully.")
                .build();
    }

    /**
     * Yêu cầu xác thực organization (bật cờ pending).
     */
    @PostMapping("/{orgId}/request-verify")
    @PreAuthorize("@orgSecurity.canManageOrganization(#orgId, authentication)")
    public ApiResponse<?> requestVerifyOrganization(@PathVariable UUID orgId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {

        organizationService.requestVerifyOrganization(orgId, userPrincipal.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization verification requested successfully.")
                .build();
    }
}