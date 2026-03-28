package com.revshop.dao.impl;

import com.revshop.dao.PaymentDAO;
import com.revshop.entity.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentDAOImpl implements PaymentDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            em.persist(payment);
            return payment;
        }
        return em.merge(payment);
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        return em.createQuery("""
                SELECT p FROM Payment p
                WHERE p.id = :paymentId
                AND p.active = true
                AND p.isDeleted = false
                """, Payment.class)
                .setParameter("paymentId", paymentId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return em.createQuery("""
                SELECT p FROM Payment p
                WHERE p.order.id = :orderId
                AND p.active = true
                AND p.isDeleted = false
                """, Payment.class)
                .setParameter("orderId", orderId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Payment> findByBuyerId(Long buyerId) {
        return em.createQuery("""
                SELECT p FROM Payment p
                WHERE p.buyer.id = :buyerId
                AND p.active = true
                AND p.isDeleted = false
                ORDER BY p.createdAt DESC
                """, Payment.class)
                .setParameter("buyerId", buyerId)
                .getResultList();
    }
}
