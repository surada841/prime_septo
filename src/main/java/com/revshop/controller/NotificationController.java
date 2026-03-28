package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.notification.MarkAllReadResponse;
import com.revshop.dto.notification.NotificationResponse;
import com.revshop.dto.notification.UnreadCountResponse;
import com.revshop.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification APIs for buyer/seller")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> myNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        List<NotificationResponse> response = notificationService.getMyNotifications(auth.getName(), unreadOnly);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", response));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(Authentication auth) {
        long unreadCount = notificationService.getUnreadCount(auth.getName());
        UnreadCountResponse response = UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Unread notification count fetched", response));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            Authentication auth
    ) {
        NotificationResponse response = notificationService.markAsRead(auth.getName(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/my/read-all")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllAsRead(Authentication auth) {
        long updated = notificationService.markAllAsRead(auth.getName());
        MarkAllReadResponse response = MarkAllReadResponse.builder()
                .updatedCount(updated)
                .build();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", response));
    }
}
