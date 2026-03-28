package com.revshop.dao;

import com.revshop.entity.WishlistItem;

import java.util.List;
import java.util.Optional;

public interface WishlistDAO {

    WishlistItem save(WishlistItem item);

    Optional<WishlistItem> findById(Long wishlistItemId);

    Optional<WishlistItem> findByBuyerIdAndProductId(Long buyerId, Long productId);

    List<WishlistItem> findByBuyerEmail(String buyerEmail);
}
