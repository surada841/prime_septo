package com.revshop.service;

import com.revshop.dto.order.OrderResponse;
import com.revshop.dto.order.PlaceOrderRequest;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(String buyerEmail, PlaceOrderRequest request);

    List<OrderResponse> getBuyerOrders(String buyerEmail);

    OrderResponse getBuyerOrderById(String buyerEmail, Long orderId);

    List<OrderResponse> getSellerOrders(String sellerEmail);

    OrderResponse cancelBuyerOrder(String buyerEmail, Long orderId, String reason);

    OrderResponse requestReturn(String buyerEmail, Long orderId, String reason);

    OrderResponse requestExchange(String buyerEmail, Long orderId, String reason, Long exchangeProductId);

    OrderResponse markOrderShippedBySeller(String sellerEmail, Long orderId);

    OrderResponse markOrderDeliveredBySeller(String sellerEmail, Long orderId);

    OrderResponse confirmOrderDeliveredByBuyer(String buyerEmail, Long orderId);

    OrderResponse completeReturnBySeller(String sellerEmail, Long orderId);

    OrderResponse completeExchangeBySeller(String sellerEmail, Long orderId);
}
