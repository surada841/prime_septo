package com.revshop.dto.wishlist;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class WishlistItemResponse {

    private Long wishlistItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private Long sellerId;
    private String sellerEmail;
    private LocalDateTime addedAt;
}
