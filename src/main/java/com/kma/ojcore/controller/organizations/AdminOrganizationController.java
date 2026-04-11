package com.kma.ojcore.controller.organizations;

import com.kma.ojcore.dto.response.common.ApiResponse;
import com.kma.ojcore.dto.response.organizations.OrganizationBasicSdo;
import com.kma.ojcore.enums.OrgApprovalStatus;
import com.kma.ojcore.security.UserPrincipal;
import com.kma.ojcore.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    /**
     * Tìm kiếm toàn bộ organization dành cho Site Staff (không giới hạn status).
     */
    @GetMapping
    public ApiResponse<?> searchAllOrganizations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrgApprovalStatus approvalStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @SortDefault Sort sort) {
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrganizationBasicSdo> result = organizationService.searchOrganizations(keyword, approvalStatus, true, pageable);

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organizations retrieved successfully.")
                .data(result)
                .build();
    }

    /**
     * Admin phê duyệt (Approve/Reject) xác thực organization.
     */
    @PatchMapping("/{orgId}/approval")
    public ApiResponse<?> approveVerifyOrganization(@PathVariable UUID orgId,
                                                    @RequestParam boolean isApproved,
                                                    @AuthenticationPrincipal UserPrincipal admin) {

        organizationService.approveVerifyOrganization(orgId, isApproved, admin.getId());

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Organization verification processed successfully.")
                .build();
    }
}