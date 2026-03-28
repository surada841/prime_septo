package com.revshop.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private Long cartId;
    private Long buyerId;
    private String buyerEmail;
    private Integer totalItems;
    private BigDecimal grandTotal;
    private List<CartItemResponse> items;
}
