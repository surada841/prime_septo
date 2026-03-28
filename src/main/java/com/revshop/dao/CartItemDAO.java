package com.revshop.dao;

import com.revshop.entity.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemDAO {

    CartItem save(CartItem cartItem);

    List<CartItem> findActiveByCartId(Long cartId);

    Optional<CartItem> findByIdAndCartId(Long itemId, Long cartId);

    Optional<CartItem> findActiveByCartIdAndProductId(Long cartId, Long productId);
}
