package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.review.CreateReviewRequest;
import com.revshop.dto.review.ProductRatingSummaryResponse;
import com.revshop.dto.review.ReviewResponse;
import com.revshop.dto.review.UpdateReviewRequest;
import com.revshop.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and rating APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication auth
    ) {
        ReviewResponse response = reviewService.createReview(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Review created", response));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication auth
    ) {
        ReviewResponse response = reviewService.updateReview(auth.getName(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", response));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            Authentication auth
    ) {
        reviewService.deleteReview(auth.getName(), reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> myReviews(Authentication auth) {
        List<ReviewResponse> response = reviewService.getMyReviews(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Buyer reviews fetched", response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> productReviews(@PathVariable Long productId) {
        List<ReviewResponse> response = reviewService.getProductReviews(productId);
        return ResponseEntity.ok(ApiResponse.success("Product reviews fetched", response));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> productRatingSummary(@PathVariable Long productId) {
        ProductRatingSummaryResponse response = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success("Product rating summary fetched", response));
    }

    @GetMapping("/seller")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> sellerProductReviews(Authentication auth) {
        List<ReviewResponse> response = reviewService.getSellerProductReviews(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Seller product reviews fetched", response));
    }
}
