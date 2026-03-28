package com.revshop.service;

import com.revshop.dto.admin.AdminSummaryResponse;
import com.revshop.dto.admin.AdminUserResponse;
import com.revshop.dto.common.PagedResponse;
import com.revshop.entity.Role;

public interface AdminService {

    AdminSummaryResponse getSummary(String authEmail, String adminKey);

    PagedResponse<AdminUserResponse> searchUsers(
            String authEmail,
            String adminKey,
            String keyword,
            Role role,
            Boolean active,
            int page,
            int size
    );

    AdminUserResponse updateUserStatus(String authEmail, String adminKey, Long userId, boolean active);

    AdminUserResponse softDeleteUser(String authEmail, String adminKey, Long userId);

    AdminUserResponse restoreUser(String authEmail, String adminKey, Long userId);
}
