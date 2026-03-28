package com.revshop.dao.impl;

import com.revshop.dao.OrderItemDAO;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class OrderItemDAOImpl implements OrderItemDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public OrderItem save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            em.persist(orderItem);
            return orderItem;
        }
        return em.merge(orderItem);
    }

    @Override
    public List<OrderItem> findByOrderId(Long orderId) {
        return em.createQuery("""
                SELECT oi FROM OrderItem oi
                WHERE oi.order.id = :orderId
                AND oi.active = true
                AND oi.isDeleted = false
                ORDER BY oi.createdAt ASC
                """, OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    @Override
    public List<OrderItem> findBySellerEmail(String sellerEmail) {
        return em.createQuery("""
                SELECT oi FROM OrderItem oi
                JOIN FETCH oi.order o
                JOIN FETCH o.buyer b
                JOIN FETCH oi.product p
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                ORDER BY o.createdAt DESC
                """, OrderItem.class)
                .setParameter("sellerEmail", sellerEmail)
                .getResultList();
    }

    @Override
    public long countDistinctOrdersBySellerEmail(String sellerEmail) {
        Long count = em.createQuery("""
                SELECT COUNT(DISTINCT o.id) FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countDistinctPendingOrdersBySellerEmail(String sellerEmail) {
        Long count = em.createQuery("""
                SELECT COUNT(DISTINCT o.id) FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                AND o.status IN :statuses
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .setParameter("statuses", List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.SHIPPED))
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long sumQuantityBySellerEmail(String sellerEmail) {
        Long count = em.createQuery("""
                SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                AND o.status IN :salesStatuses
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .setParameter("salesStatuses", List.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURN_REQUESTED,
                        OrderStatus.EXCHANGE_REQUESTED,
                        OrderStatus.EXCHANGED
                ))
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public BigDecimal sumRevenueBySellerEmail(String sellerEmail) {
        BigDecimal total = em.createQuery("""
                SELECT COALESCE(SUM(oi.lineTotal), 0) FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                AND o.status IN :salesStatuses
                """, BigDecimal.class)
                .setParameter("sellerEmail", sellerEmail)
                .setParameter("salesStatuses", List.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURN_REQUESTED,
                        OrderStatus.EXCHANGE_REQUESTED,
                        OrderStatus.EXCHANGED
                ))
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public List<Object[]> findTopProductsBySellerEmail(String sellerEmail, int limit) {
        return em.createQuery("""
                SELECT p.id, p.name, p.stock, SUM(oi.quantity), COALESCE(SUM(oi.lineTotal), 0)
                FROM OrderItem oi
                JOIN oi.order o
                JOIN oi.product p
                WHERE oi.seller.email = :sellerEmail
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                AND o.status IN :salesStatuses
                GROUP BY p.id, p.name, p.stock
                ORDER BY SUM(oi.lineTotal) DESC
                """, Object[].class)
                .setParameter("sellerEmail", sellerEmail)
                .setParameter("salesStatuses", List.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURN_REQUESTED,
                        OrderStatus.EXCHANGE_REQUESTED,
                        OrderStatus.EXCHANGED
                ))
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }
}
