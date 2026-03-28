package com.revshop.service;

import com.revshop.dto.notification.NotificationResponse;
import com.revshop.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    void createNotification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId
    );

    List<NotificationResponse> getMyNotifications(String userEmail, boolean unreadOnly);

    NotificationResponse markAsRead(String userEmail, Long notificationId);

    long markAllAsRead(String userEmail);

    long getUnreadCount(String userEmail);
}
