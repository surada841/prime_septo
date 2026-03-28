package com.revshop.dao;

import com.revshop.entity.Review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ReviewDAO {

    Review save(Review review);

    Optional<Review> findById(Long reviewId);

    Optional<Review> findByBuyerIdAndProductId(Long buyerId, Long productId);

    List<Review> findByProductId(Long productId);

    List<Review> findByBuyerEmail(String buyerEmail);

    List<Review> findBySellerEmail(String sellerEmail);

    boolean hasBuyerPurchasedProduct(String buyerEmail, Long productId);

    long countByProductId(Long productId);

    BigDecimal averageRatingByProductId(Long productId);
}
