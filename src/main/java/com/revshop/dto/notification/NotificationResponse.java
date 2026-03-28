package com.revshop.dto.notification;

import com.revshop.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
