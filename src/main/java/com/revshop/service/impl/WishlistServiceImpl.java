package com.revshop.service.impl;

import com.revshop.dao.ProductDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dao.WishlistDAO;
import com.revshop.dto.wishlist.WishlistItemResponse;
import com.revshop.dto.wishlist.WishlistStatusResponse;
import com.revshop.entity.Product;
import com.revshop.entity.ProductImage;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.entity.WishlistItem;
import com.revshop.exception.ConflictException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistDAO wishlistDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    @Override
    @Transactional
    public WishlistItemResponse addItem(String buyerEmail, Long productId) {
        User buyer = getActiveBuyer(buyerEmail);
        Product product = getActiveProduct(productId);

        WishlistItem existing = wishlistDAO.findByBuyerIdAndProductId(buyer.getId(), productId).orElse(null);
        if (existing != null && Boolean.TRUE.equals(existing.getActive()) && Boolean.FALSE.equals(existing.getIsDeleted())) {
            throw new ConflictException("Product already exists in wishlist");
        }

        if (existing != null) {
            existing.setActive(true);
            existing.setIsDeleted(false);
            return mapToResponse(wishlistDAO.save(existing));
        }

        WishlistItem item = WishlistItem.builder()
                .buyer(buyer)
                .product(product)
                .active(true)
                .build();
        return mapToResponse(wishlistDAO.save(item));
    }

    @Override
    @Transactional
    public void removeItem(String buyerEmail, Long wishlistItemId) {
        User buyer = getActiveBuyer(buyerEmail);
        WishlistItem item = wishlistDAO.findById(wishlistItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        if (!item.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenOperationException("Not owner of wishlist item");
        }

        item.setActive(false);
        item.setIsDeleted(true);
        wishlistDAO.save(item);
    }

    @Override
    @Transactional
    public void removeProduct(String buyerEmail, Long productId) {
        User buyer = getActiveBuyer(buyerEmail);
        WishlistItem item = wishlistDAO.findByBuyerIdAndProductId(buyer.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        if (!Boolean.TRUE.equals(item.getActive()) || Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new ResourceNotFoundException("Wishlist item not found");
        }

        item.setActive(false);
        item.setIsDeleted(true);
        wishlistDAO.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist(String buyerEmail) {
        User buyer = getActiveBuyer(buyerEmail);
        return wishlistDAO.findByBuyerEmail(buyer.getEmail())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatusResponse getWishlistStatus(String buyerEmail, Long productId) {
        User buyer = getActiveBuyer(buyerEmail);
        WishlistItem item = wishlistDAO.findByBuyerIdAndProductId(buyer.getId(), productId).orElse(null);
        boolean inWishlist = item != null && Boolean.TRUE.equals(item.getActive()) && Boolean.FALSE.equals(item.getIsDeleted());

        return WishlistStatusResponse.builder()
                .productId(productId)
                .inWishlist(inWishlist)
                .build();
    }

    private User getActiveBuyer(String buyerEmail) {
        User buyer = userDAO.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        if (buyer.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can access wishlist");
        }
        if (!Boolean.TRUE.equals(buyer.getActive()) || Boolean.TRUE.equals(buyer.getIsDeleted())) {
            throw new ForbiddenOperationException("Buyer account is inactive");
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

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        String imageUrl = null;
        List<ProductImage> images = item.getProduct().getImages();
        if (images != null && !images.isEmpty()) {
            imageUrl = images.stream()
                    .filter(i -> i.getDisplayOrder() != null)
                    .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }

        return WishlistItemResponse.builder()
                .wishlistItemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(imageUrl)
                .productPrice(item.getProduct().getPrice())
                .sellerId(item.getProduct().getSeller().getId())
                .sellerEmail(item.getProduct().getSeller().getEmail())
                .addedAt(item.getCreatedAt())
                .build();
    }
}
