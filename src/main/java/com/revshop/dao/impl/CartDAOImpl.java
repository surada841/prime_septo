package com.revshop.dao.impl;

import com.revshop.dao.CartDAO;
import com.revshop.entity.Cart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CartDAOImpl implements CartDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            em.persist(cart);
            return cart;
        }
        return em.merge(cart);
    }

    @Override
    public Optional<Cart> findById(Long cartId) {
        return em.createQuery("""
                SELECT c FROM Cart c
                WHERE c.id = :cartId
                AND c.active = true
                AND c.isDeleted = false
                """, Cart.class)
                .setParameter("cartId", cartId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Cart> findByBuyerId(Long buyerId) {
        return em.createQuery("""
                SELECT c FROM Cart c
                WHERE c.buyer.id = :buyerId
                AND c.active = true
                AND c.isDeleted = false
                """, Cart.class)
                .setParameter("buyerId", buyerId)
                .getResultStream()
                .findFirst();
    }
}
