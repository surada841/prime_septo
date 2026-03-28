package com.revshop.controller;

import com.revshop.dto.cart.AddToCartRequest;
import com.revshop.dto.cart.CartResponse;
import com.revshop.dto.cart.UpdateCartItemRequest;
import com.revshop.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.CartService;
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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Buyer cart operations")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication auth) {
        CartResponse response = cartService.getCart(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication auth
    ) {
        CartResponse response = cartService.addToCart(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication auth
    ) {
        CartResponse response = cartService.updateCartItem(auth.getName(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable Long itemId,
            Authentication auth
    ) {
        CartResponse response = cartService.removeCartItem(auth.getName(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Cart item removed", response));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(Authentication auth) {
        CartResponse response = cartService.clearCart(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", response));
    }
}
