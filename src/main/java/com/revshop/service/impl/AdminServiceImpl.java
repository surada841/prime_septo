package com.revshop.service.impl;

import com.revshop.dao.AdminDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.admin.AdminSummaryResponse;
import com.revshop.dto.admin.AdminUserResponse;
import com.revshop.dto.common.PagedResponse;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminDAO adminDAO;
    private final UserDAO userDAO;

    @Value("${app.admin.api-key:change-me-admin-key}")
    private String configuredAdminKey;

    @Override
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary(String authEmail, String adminKey) {
        validateAdminAccess(authEmail, adminKey);

        return AdminSummaryResponse.builder()
                .totalUsers(adminDAO.countUsers(true))
                .totalBuyers(adminDAO.countUsersByRole(Role.BUYER, true))
                .totalSellers(adminDAO.countUsersByRole(Role.SELLER, true))
                .activeUsers(adminDAO.countUsersByActive(true, true))
                .deletedUsers(adminDAO.countDeletedUsers())
                .totalProducts(adminDAO.countProducts())
                .totalOrders(adminDAO.countOrders())
                .totalPayments(adminDAO.countPayments())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> searchUsers(
            String authEmail,
            String adminKey,
            String keyword,
            Role role,
            Boolean active,
            int page,
            int size
    ) {
        validateAdminAccess(authEmail, adminKey);

        if (page < 0) {
            throw new BadRequestException("Page must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new BadRequestException("Size must be between 1 and 100");
        }

        int offset = page * size;
        List<AdminUserResponse> content = adminDAO.searchUsers(keyword, role, active, offset, size)
                .stream()
                .map(this::toAdminUserResponse)
                .toList();

        long totalElements = adminDAO.countSearchUsers(keyword, role, active);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PagedResponse.<AdminUserResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .sortBy("createdAt")
                .sortDir("desc")
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(String authEmail, String adminKey, Long userId, boolean active) {
        validateAdminAccess(authEmail, adminKey);
        User user = adminDAO.findUserByIdAnyState(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(active);
        adminDAO.saveUser(user);
        return toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse softDeleteUser(String authEmail, String adminKey, Long userId) {
        validateAdminAccess(authEmail, adminKey);
        User user = adminDAO.findUserByIdAnyState(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        user.setIsDeleted(true);
        adminDAO.saveUser(user);
        return toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse restoreUser(String authEmail, String adminKey, Long userId) {
        validateAdminAccess(authEmail, adminKey);
        User user = adminDAO.findUserByIdAnyState(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsDeleted(false);
        user.setActive(true);
        adminDAO.saveUser(user);
        return toAdminUserResponse(user);
    }

    private void validateAdminAccess(String authEmail, String adminKey) {
        User caller = userDAO.findByEmail(authEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (caller.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only privileged seller account can access admin APIs");
        }

        if (configuredAdminKey == null || configuredAdminKey.isBlank()) {
            throw new ForbiddenOperationException("Admin API key is not configured");
        }

        if (adminKey == null || !configuredAdminKey.equals(adminKey)) {
            throw new ForbiddenOperationException("Invalid admin key");
        }
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .deleted(user.getIsDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
