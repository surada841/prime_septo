package com.revshop.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {

    private Long itemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
