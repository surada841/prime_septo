package com.revshop.service.impl;

import com.revshop.dao.CartDAO;
import com.revshop.dao.CartItemDAO;
import com.revshop.dao.ProductDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.cart.AddToCartRequest;
import com.revshop.dto.cart.CartItemResponse;
import com.revshop.dto.cart.CartResponse;
import com.revshop.dto.cart.UpdateCartItemRequest;
import com.revshop.entity.Cart;
import com.revshop.entity.CartItem;
import com.revshop.entity.Product;
import com.revshop.entity.ProductImage;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartDAO cartDAO;
    private final CartItemDAO cartItemDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    @Override
    @Transactional
    public CartResponse getCart(String buyerEmail) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);
        return buildCartResponse(cart, buyer);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String buyerEmail, AddToCartRequest request) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);

        Product product = getValidProductForCart(request.getProductId());

        CartItem item = cartItemDAO.findActiveByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(0)
                        .unitPrice(product.getPrice())
                        .active(true)
                        .build());

        int newQty = item.getQuantity() + request.getQuantity();
        validateStock(product, newQty);

        item.setQuantity(newQty);
        item.setUnitPrice(product.getPrice());
        item.setActive(true);
        item.setIsDeleted(false);
        cartItemDAO.save(item);

        return buildCartResponse(cart, buyer);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(String buyerEmail, Long itemId, UpdateCartItemRequest request) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);

        CartItem item = cartItemDAO.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Product product = getValidProductForCart(item.getProduct().getId());
        validateStock(product, request.getQuantity());

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(product.getPrice());
        cartItemDAO.save(item);

        return buildCartResponse(cart, buyer);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(String buyerEmail, Long itemId) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);

        CartItem item = cartItemDAO.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        item.setActive(false);
        item.setIsDeleted(true);
        cartItemDAO.save(item);

        return buildCartResponse(cart, buyer);
    }

    @Override
    @Transactional
    public CartResponse clearCart(String buyerEmail) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = getOrCreateCart(buyer);

        List<CartItem> items = cartItemDAO.findActiveByCartId(cart.getId());
        items.forEach(item -> {
            item.setActive(false);
            item.setIsDeleted(true);
            cartItemDAO.save(item);
        });

        return buildCartResponse(cart, buyer);
    }

    private User getValidatedBuyer(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can access cart");
        }

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("Buyer account is inactive");
        }

        return user;
    }

    private Cart getOrCreateCart(User buyer) {
        return cartDAO.findByBuyerId(buyer.getId())
                .orElseGet(() -> cartDAO.save(Cart.builder()
                        .buyer(buyer)
                        .active(true)
                        .build()));
    }

    private Product getValidProductForCart(Long productId) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getActive()) || Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new BadRequestException("Product is not available");
        }

        if (!Boolean.TRUE.equals(product.getInStock()) || product.getStock() <= 0) {
            throw new BadRequestException("Product is out of stock");
        }

        return product;
    }

    private void validateStock(Product product, Integer quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        if (quantity > product.getStock()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
    }

    private CartResponse buildCartResponse(Cart cart, User buyer) {
        List<CartItemResponse> items = cartItemDAO.findActiveByCartId(cart.getId())
                .stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal grandTotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .buyerId(buyer.getId())
                .buyerEmail(buyer.getEmail())
                .totalItems(totalItems)
                .grandTotal(grandTotal)
                .items(items)
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

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

        return CartItemResponse.builder()
                .itemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(imageUrl)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(lineTotal)
                .build();
    }
}
