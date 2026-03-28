package com.revshop.dao;

import com.revshop.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationDAO {

    Notification save(Notification notification);

    Optional<Notification> findById(Long notificationId);

    List<Notification> findByRecipientEmail(String recipientEmail);

    List<Notification> findUnreadByRecipientEmail(String recipientEmail);

    long countUnreadByRecipientEmail(String recipientEmail);

    long markAllAsReadByRecipientId(Long recipientId);
}
