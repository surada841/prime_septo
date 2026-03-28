package com.revshop.dto.wishlist;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistStatusResponse {

    private Long productId;
    private Boolean inWishlist;
}
