package com.revshop.dao.impl;

import com.revshop.dao.OrderDAO;
import com.revshop.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderDAOImpl implements OrderDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return em.createQuery("""
                SELECT o FROM CustomerOrder o
                WHERE o.id = :orderId
                AND o.active = true
                AND o.isDeleted = false
                """, Order.class)
                .setParameter("orderId", orderId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Order> findByBuyerId(Long buyerId) {
        return em.createQuery("""
                SELECT o FROM CustomerOrder o
                WHERE o.buyer.id = :buyerId
                AND o.active = true
                AND o.isDeleted = false
                ORDER BY o.createdAt DESC
                """, Order.class)
                .setParameter("buyerId", buyerId)
                .getResultList();
    }

    @Override
    public List<Order> findBySellerEmail(String sellerEmail) {
        return em.createQuery("""
                SELECT DISTINCT o FROM CustomerOrder o
                JOIN o.items oi
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                ORDER BY o.createdAt DESC
                """, Order.class)
                .setParameter("sellerEmail", sellerEmail)
                .getResultList();
    }
}
