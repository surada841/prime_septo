package com.revshop.service.impl;

import com.revshop.dao.ProductDAO;
import com.revshop.dao.ReviewDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.review.CreateReviewRequest;
import com.revshop.dto.review.ProductRatingSummaryResponse;
import com.revshop.dto.review.ReviewResponse;
import com.revshop.dto.review.UpdateReviewRequest;
import com.revshop.entity.Product;
import com.revshop.entity.Review;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.ConflictException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewDAO reviewDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    @Override
    @Transactional
    public ReviewResponse createReview(String buyerEmail, CreateReviewRequest request) {
        User buyer = getActiveBuyer(buyerEmail);
        Product product = getActiveProduct(request.getProductId());

        if (!reviewDAO.hasBuyerPurchasedProduct(buyerEmail, product.getId())) {
            throw new ForbiddenOperationException("You can review only products you have purchased");
        }

        reviewDAO.findByBuyerIdAndProductId(buyer.getId(), product.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("Review already exists for this product by this buyer");
                });

        Review review = Review.builder()
                .buyer(buyer)
                .product(product)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .active(true)
                .build();

        return mapToResponse(reviewDAO.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String buyerEmail, Long reviewId, UpdateReviewRequest request) {
        User buyer = getActiveBuyer(buyerEmail);
        Review review = reviewDAO.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenOperationException("Not owner of this review");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());

        return mapToResponse(reviewDAO.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(String buyerEmail, Long reviewId) {
        User buyer = getActiveBuyer(buyerEmail);
        Review review = reviewDAO.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenOperationException("Not owner of this review");
        }

        review.setActive(false);
        review.setIsDeleted(true);
        reviewDAO.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(String buyerEmail) {
        User buyer = getActiveBuyer(buyerEmail);
        return reviewDAO.findByBuyerEmail(buyer.getEmail())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        getActiveProduct(productId);
        return reviewDAO.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        Product product = getActiveProduct(productId);
        long totalReviews = reviewDAO.countByProductId(productId);
        BigDecimal averageRating = reviewDAO.averageRatingByProductId(productId);

        return ProductRatingSummaryResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getSellerProductReviews(String sellerEmail) {
        User seller = userDAO.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only seller can view seller product reviews");
        }
        return reviewDAO.findBySellerEmail(sellerEmail)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User getActiveBuyer(String buyerEmail) {
        User buyer = userDAO.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        if (buyer.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can perform this action");
        }
        return buyer;
    }

    private Product getActiveProduct(Long productId) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!Boolean.TRUE.equals(product.getActive()) || Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new ResourceNotFoundException("Product not found");
        }
        return product;
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .buyerId(review.getBuyer().getId())
                .buyerEmail(review.getBuyer().getEmail())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
