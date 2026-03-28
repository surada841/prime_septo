package com.revshop.dao.impl;

import com.revshop.dao.CartItemDAO;
import com.revshop.entity.CartItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CartItemDAOImpl implements CartItemDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            em.persist(cartItem);
            return cartItem;
        }
        return em.merge(cartItem);
    }

    @Override
    public List<CartItem> findActiveByCartId(Long cartId) {
        return em.createQuery("""
                SELECT ci FROM CartItem ci
                WHERE ci.cart.id = :cartId
                AND ci.active = true
                AND ci.isDeleted = false
                ORDER BY ci.createdAt ASC
                """, CartItem.class)
                .setParameter("cartId", cartId)
                .getResultList();
    }

    @Override
    public Optional<CartItem> findByIdAndCartId(Long itemId, Long cartId) {
        return em.createQuery("""
                SELECT ci FROM CartItem ci
                WHERE ci.id = :itemId
                AND ci.cart.id = :cartId
                AND ci.active = true
                AND ci.isDeleted = false
                """, CartItem.class)
                .setParameter("itemId", itemId)
                .setParameter("cartId", cartId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<CartItem> findActiveByCartIdAndProductId(Long cartId, Long productId) {
        return em.createQuery("""
                SELECT ci FROM CartItem ci
                WHERE ci.cart.id = :cartId
                AND ci.product.id = :productId
                AND ci.active = true
                AND ci.isDeleted = false
                """, CartItem.class)
                .setParameter("cartId", cartId)
                .setParameter("productId", productId)
                .getResultStream()
                .findFirst();
    }
}
