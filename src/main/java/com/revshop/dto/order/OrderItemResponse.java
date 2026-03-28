package com.revshop.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long orderItemId;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerEmail;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
