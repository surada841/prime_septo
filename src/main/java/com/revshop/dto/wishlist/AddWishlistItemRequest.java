package com.revshop.dto.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWishlistItemRequest {

    @NotNull(message = "Product id is required")
    private Long productId;
}
