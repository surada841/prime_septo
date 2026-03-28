package com.revshop.dao;

import com.revshop.entity.Cart;

import java.util.Optional;

public interface CartDAO {

    Cart save(Cart cart);

    Optional<Cart> findById(Long cartId);

    Optional<Cart> findByBuyerId(Long buyerId);
}
