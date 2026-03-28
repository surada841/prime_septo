package com.revshop.service;

import com.revshop.dto.wishlist.WishlistItemResponse;
import com.revshop.dto.wishlist.WishlistStatusResponse;

import java.util.List;

public interface WishlistService {

    WishlistItemResponse addItem(String buyerEmail, Long productId);

    void removeItem(String buyerEmail, Long wishlistItemId);

    void removeProduct(String buyerEmail, Long productId);

    List<WishlistItemResponse> getMyWishlist(String buyerEmail);

    WishlistStatusResponse getWishlistStatus(String buyerEmail, Long productId);
}
