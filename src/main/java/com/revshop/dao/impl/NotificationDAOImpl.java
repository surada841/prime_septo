package com.revshop.dao.impl;

import com.revshop.dao.NotificationDAO;
import com.revshop.entity.Notification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NotificationDAOImpl implements NotificationDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            em.persist(notification);
            return notification;
        }
        return em.merge(notification);
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        return em.createQuery("""
                SELECT n FROM Notification n
                JOIN FETCH n.recipient r
                WHERE n.id = :notificationId
                AND n.active = true
                AND n.isDeleted = false
                AND r.isDeleted = false
                """, Notification.class)
                .setParameter("notificationId", notificationId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Notification> findByRecipientEmail(String recipientEmail) {
        return em.createQuery("""
                SELECT n FROM Notification n
                JOIN FETCH n.recipient r
                WHERE r.email = :recipientEmail
                AND n.active = true
                AND n.isDeleted = false
                AND r.isDeleted = false
                ORDER BY n.createdAt DESC
                """, Notification.class)
                .setParameter("recipientEmail", recipientEmail)
                .getResultList();
    }

    @Override
    public List<Notification> findUnreadByRecipientEmail(String recipientEmail) {
        return em.createQuery("""
                SELECT n FROM Notification n
                JOIN FETCH n.recipient r
                WHERE r.email = :recipientEmail
                AND n.active = true
                AND n.isDeleted = false
                AND n.isRead = false
                AND r.isDeleted = false
                ORDER BY n.createdAt DESC
                """, Notification.class)
                .setParameter("recipientEmail", recipientEmail)
                .getResultList();
    }

    @Override
    public long countUnreadByRecipientEmail(String recipientEmail) {
        return em.createQuery("""
                SELECT COUNT(n) FROM Notification n
                JOIN n.recipient r
                WHERE r.email = :recipientEmail
                AND n.active = true
                AND n.isDeleted = false
                AND n.isRead = false
                AND r.isDeleted = false
                """, Long.class)
                .setParameter("recipientEmail", recipientEmail)
                .getSingleResult();
    }

    @Override
    public long markAllAsReadByRecipientId(Long recipientId) {
        return em.createQuery("""
                UPDATE Notification n
                SET n.isRead = true
                WHERE n.recipient.id = :recipientId
                AND n.active = true
                AND n.isDeleted = false
                AND n.isRead = false
                """)
                .setParameter("recipientId", recipientId)
                .executeUpdate();
    }
}
