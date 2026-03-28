package com.revshop.service;

import com.revshop.dto.review.CreateReviewRequest;
import com.revshop.dto.review.ProductRatingSummaryResponse;
import com.revshop.dto.review.ReviewResponse;
import com.revshop.dto.review.UpdateReviewRequest;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(String buyerEmail, CreateReviewRequest request);

    ReviewResponse updateReview(String buyerEmail, Long reviewId, UpdateReviewRequest request);

    void deleteReview(String buyerEmail, Long reviewId);

    List<ReviewResponse> getMyReviews(String buyerEmail);

    List<ReviewResponse> getProductReviews(Long productId);

    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    List<ReviewResponse> getSellerProductReviews(String sellerEmail);
}
