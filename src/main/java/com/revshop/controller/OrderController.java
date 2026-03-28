package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.order.CancelOrderRequest;
import com.revshop.dto.order.ExchangeOrderRequest;
import com.revshop.dto.order.OrderResponse;
import com.revshop.dto.order.PlaceOrderRequest;
import com.revshop.dto.order.ReturnOrderRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and order history APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication auth
    ) {
        OrderResponse response = orderService.placeOrder(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> myOrders(Authentication auth) {
        List<OrderResponse> response = orderService.getBuyerOrders(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Buyer orders fetched", response));
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> myOrderById(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.getBuyerOrderById(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Buyer order fetched", response));
    }

    @GetMapping("/seller")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> sellerOrders(Authentication auth) {
        List<OrderResponse> response = orderService.getSellerOrders(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Seller orders fetched", response));
    }

    @PatchMapping("/seller/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> markShipped(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.markOrderShippedBySeller(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as shipped", response));
    }

    @PatchMapping("/seller/{orderId}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.markOrderDeliveredBySeller(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered", response));
    }

    @PatchMapping("/seller/{orderId}/return/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeReturn(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.completeReturnBySeller(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Return request completed", response));
    }

    @PatchMapping("/seller/{orderId}/exchange/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeExchange(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.completeExchangeBySeller(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Exchange request completed", response));
    }

    @PatchMapping("/my/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request,
            Authentication auth
    ) {
        String reason = request == null ? null : request.getReason();
        OrderResponse response = orderService.cancelBuyerOrder(auth.getName(), orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    @PatchMapping("/my/{orderId}/return")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) ReturnOrderRequest request,
            Authentication auth
    ) {
        String reason = request == null ? null : request.getReason();
        OrderResponse response = orderService.requestReturn(auth.getName(), orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted", response));
    }

    @PatchMapping("/my/{orderId}/exchange")
    public ResponseEntity<ApiResponse<OrderResponse>> requestExchange(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) ExchangeOrderRequest request,
            Authentication auth
    ) {
        String reason = request == null ? null : request.getReason();
        Long exchangeProductId = request == null ? null : request.getExchangeProductId();
        OrderResponse response = orderService.requestExchange(auth.getName(), orderId, reason, exchangeProductId);
        return ResponseEntity.ok(ApiResponse.success("Exchange request submitted", response));
    }

    @PatchMapping("/my/{orderId}/confirm-delivery")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmDelivery(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        OrderResponse response = orderService.confirmOrderDeliveredByBuyer(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Delivery confirmed", response));
    }
}
