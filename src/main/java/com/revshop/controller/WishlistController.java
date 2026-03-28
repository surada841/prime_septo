package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.wishlist.AddWishlistItemRequest;
import com.revshop.dto.wishlist.WishlistItemResponse;
import com.revshop.dto.wishlist.WishlistStatusResponse;
import com.revshop.service.WishlistService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Buyer wishlist/favorite product APIs")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addItem(
            @Valid @RequestBody AddWishlistItemRequest request,
            Authentication auth
    ) {
        WishlistItemResponse response = wishlistService.addItem(auth.getName(), request.getProductId());
        return ResponseEntity.ok(ApiResponse.success("Wishlist item added", response));
    }

    @DeleteMapping("/items/{wishlistItemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable Long wishlistItemId,
            Authentication auth
    ) {
        wishlistService.removeItem(auth.getName(), wishlistItemId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist item removed", null));
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeProduct(
            @PathVariable Long productId,
            Authentication auth
    ) {
        wishlistService.removeProduct(auth.getName(), productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist product removed", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> myWishlist(Authentication auth) {
        List<WishlistItemResponse> response = wishlistService.getMyWishlist(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", response));
    }

    @GetMapping("/status/{productId}")
    public ResponseEntity<ApiResponse<WishlistStatusResponse>> wishlistStatus(
            @PathVariable Long productId,
            Authentication auth
    ) {
        WishlistStatusResponse response = wishlistService.getWishlistStatus(auth.getName(), productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist status fetched", response));
    }
}
