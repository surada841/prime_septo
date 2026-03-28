package com.revshop.service.impl;

import com.revshop.dao.NotificationDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.notification.NotificationResponse;
import com.revshop.entity.Notification;
import com.revshop.entity.NotificationType;
import com.revshop.entity.User;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;

    @Override
    @Transactional
    public void createNotification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId
    ) {
        User recipient = userDAO.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found"));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .active(true)
                .build();
        notificationDAO.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userEmail, boolean unreadOnly) {
        User user = getActiveUserByEmail(userEmail);
        List<Notification> notifications = unreadOnly
                ? notificationDAO.findUnreadByRecipientEmail(user.getEmail())
                : notificationDAO.findByRecipientEmail(user.getEmail());

        return notifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String userEmail, Long notificationId) {
        User user = getActiveUserByEmail(userEmail);
        Notification notification = notificationDAO.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("Notification does not belong to user");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationDAO.save(notification);
        }
        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public long markAllAsRead(String userEmail) {
        User user = getActiveUserByEmail(userEmail);
        return notificationDAO.markAllAsReadByRecipientId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        User user = getActiveUserByEmail(userEmail);
        return notificationDAO.countUnreadByRecipientEmail(user.getEmail());
    }

    private User getActiveUserByEmail(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("User account is inactive");
        }
        return user;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
