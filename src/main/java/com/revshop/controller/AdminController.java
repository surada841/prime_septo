package com.revshop.controller;

import com.revshop.dto.admin.AdminSummaryResponse;
import com.revshop.dto.admin.AdminUserResponse;
import com.revshop.dto.admin.AdminUserStatusUpdateRequest;
import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.common.PagedResponse;
import com.revshop.entity.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Privileged admin-ready APIs protected by JWT + X-ADMIN-KEY")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminSummaryResponse>> summary(
            Authentication auth,
            @RequestHeader("X-ADMIN-KEY") String adminKey
    ) {
        AdminSummaryResponse response = adminService.getSummary(auth.getName(), adminKey);
        return ResponseEntity.ok(ApiResponse.success("Admin summary fetched", response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> users(
            Authentication auth,
            @RequestHeader("X-ADMIN-KEY") String adminKey,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<AdminUserResponse> response = adminService.searchUsers(
                auth.getName(),
                adminKey,
                keyword,
                role,
                active,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Admin users fetched", response));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            Authentication auth,
            @RequestHeader("X-ADMIN-KEY") String adminKey,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        AdminUserResponse response = adminService.updateUserStatus(
                auth.getName(),
                adminKey,
                userId,
                request.getActive()
        );
        return ResponseEntity.ok(ApiResponse.success("Admin user status updated", response));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> softDeleteUser(
            Authentication auth,
            @RequestHeader("X-ADMIN-KEY") String adminKey,
            @PathVariable Long userId
    ) {
        AdminUserResponse response = adminService.softDeleteUser(auth.getName(), adminKey, userId);
        return ResponseEntity.ok(ApiResponse.success("Admin user soft deleted", response));
    }

    @PatchMapping("/users/{userId}/restore")
    public ResponseEntity<ApiResponse<AdminUserResponse>> restoreUser(
            Authentication auth,
            @RequestHeader("X-ADMIN-KEY") String adminKey,
            @PathVariable Long userId
    ) {
        AdminUserResponse response = adminService.restoreUser(auth.getName(), adminKey, userId);
        return ResponseEntity.ok(ApiResponse.success("Admin user restored", response));
    }
}
