package com.revshop.dao.impl;

import com.revshop.dao.WishlistDAO;
import com.revshop.entity.WishlistItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WishlistDAOImpl implements WishlistDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public WishlistItem save(WishlistItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        }
        return em.merge(item);
    }

    @Override
    public Optional<WishlistItem> findById(Long wishlistItemId) {
        return em.createQuery("""
                SELECT wi FROM WishlistItem wi
                JOIN FETCH wi.buyer b
                JOIN FETCH wi.product p
                WHERE wi.id = :wishlistItemId
                AND wi.active = true
                AND wi.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                """, WishlistItem.class)
                .setParameter("wishlistItemId", wishlistItemId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<WishlistItem> findByBuyerIdAndProductId(Long buyerId, Long productId) {
        return em.createQuery("""
                SELECT wi FROM WishlistItem wi
                WHERE wi.buyer.id = :buyerId
                AND wi.product.id = :productId
                AND wi.isDeleted = false
                """, WishlistItem.class)
                .setParameter("buyerId", buyerId)
                .setParameter("productId", productId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<WishlistItem> findByBuyerEmail(String buyerEmail) {
        return em.createQuery("""
                SELECT wi FROM WishlistItem wi
                JOIN FETCH wi.buyer b
                JOIN FETCH wi.product p
                WHERE b.email = :buyerEmail
                AND wi.active = true
                AND wi.isDeleted = false
                AND b.isDeleted = false
                AND p.isDeleted = false
                ORDER BY wi.createdAt DESC
                """, WishlistItem.class)
                .setParameter("buyerEmail", buyerEmail)
                .getResultList();
    }
}
