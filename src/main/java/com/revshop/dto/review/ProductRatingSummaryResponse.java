package com.revshop.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductRatingSummaryResponse {

    private Long productId;
    private String productName;
    private long totalReviews;
    private BigDecimal averageRating;
}
