package com.revshop.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long productId;
    private String productName;
    private Long buyerId;
    private String buyerEmail;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
