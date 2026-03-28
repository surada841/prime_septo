package com.revshop.dao.impl;

import com.revshop.dao.ReviewDAO;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewDAOImpl implements ReviewDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Review save(Review review) {
        if (review.getId() == null) {
            em.persist(review);
            return review;
        }
        return em.merge(review);
    }

    @Override
    public Optional<Review> findById(Long reviewId) {
        return em.createQuery("""
                SELECT r FROM Review r
                JOIN FETCH r.buyer b
                JOIN FETCH r.product p
                WHERE r.id = :reviewId
                AND r.active = true
                AND r.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                """, Review.class)
                .setParameter("reviewId", reviewId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Review> findByBuyerIdAndProductId(Long buyerId, Long productId) {
        return em.createQuery("""
                SELECT r FROM Review r
                WHERE r.buyer.id = :buyerId
                AND r.product.id = :productId
                AND r.active = true
                AND r.isDeleted = false
                """, Review.class)
                .setParameter("buyerId", buyerId)
                .setParameter("productId", productId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Review> findByProductId(Long productId) {
        return em.createQuery("""
                SELECT r FROM Review r
                JOIN FETCH r.buyer b
                JOIN FETCH r.product p
                WHERE r.product.id = :productId
                AND r.active = true
                AND r.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                ORDER BY r.createdAt DESC
                """, Review.class)
                .setParameter("productId", productId)
                .getResultList();
    }

    @Override
    public List<Review> findByBuyerEmail(String buyerEmail) {
        return em.createQuery("""
                SELECT r FROM Review r
                JOIN FETCH r.buyer b
                JOIN FETCH r.product p
                WHERE b.email = :buyerEmail
                AND r.active = true
                AND r.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                ORDER BY r.createdAt DESC
                """, Review.class)
                .setParameter("buyerEmail", buyerEmail)
                .getResultList();
    }

    @Override
    public List<Review> findBySellerEmail(String sellerEmail) {
        return em.createQuery("""
                SELECT r FROM Review r
                JOIN FETCH r.buyer b
                JOIN FETCH r.product p
                WHERE p.seller.email = :sellerEmail
                AND r.active = true
                AND r.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                ORDER BY r.createdAt DESC
                """, Review.class)
                .setParameter("sellerEmail", sellerEmail)
                .getResultList();
    }

    @Override
    public boolean hasBuyerPurchasedProduct(String buyerEmail, Long productId) {
        Long count = em.createQuery("""
                SELECT COUNT(oi) FROM OrderItem oi
                JOIN oi.order o
                JOIN o.buyer b
                WHERE b.email = :buyerEmail
                AND oi.product.id = :productId
                AND oi.active = true
                AND oi.isDeleted = false
                AND o.active = true
                AND o.isDeleted = false
                AND o.status IN :allowedStatuses
                """, Long.class)
                .setParameter("buyerEmail", buyerEmail)
                .setParameter("productId", productId)
                .setParameter("allowedStatuses", List.of(
                        OrderStatus.DELIVERED,
                        OrderStatus.RETURNED,
                        OrderStatus.EXCHANGED
                ))
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public long countByProductId(Long productId) {
        Long count = em.createQuery("""
                SELECT COUNT(r) FROM Review r
                WHERE r.product.id = :productId
                AND r.active = true
                AND r.isDeleted = false
                """, Long.class)
                .setParameter("productId", productId)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public BigDecimal averageRatingByProductId(Long productId) {
        Double avg = em.createQuery("""
                SELECT AVG(r.rating) FROM Review r
                WHERE r.product.id = :productId
                AND r.active = true
                AND r.isDeleted = false
                """, Double.class)
                .setParameter("productId", productId)
                .getSingleResult();
        if (avg == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
