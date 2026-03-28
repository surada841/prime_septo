package com.revshop.dto.seller;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SellerTopProductResponse {

    private Long productId;
    private String productName;
    private Integer currentStock;
    private Long unitsSold;
    private BigDecimal revenue;
}
