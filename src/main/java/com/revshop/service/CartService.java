package com.revshop.service;

import com.revshop.dto.cart.AddToCartRequest;
import com.revshop.dto.cart.CartResponse;
import com.revshop.dto.cart.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(String buyerEmail);

    CartResponse addToCart(String buyerEmail, AddToCartRequest request);

    CartResponse updateCartItem(String buyerEmail, Long itemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(String buyerEmail, Long itemId);

    CartResponse clearCart(String buyerEmail);
}
