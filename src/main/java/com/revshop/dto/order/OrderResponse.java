package com.revshop.dto.order;

import com.revshop.entity.OrderStatus;
import com.revshop.entity.PaymentMethod;
import com.revshop.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private Long buyerId;
    private String buyerEmail;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String billingAddress;
    private BigDecimal totalAmount;
    private String cancelReason;
    private String returnReason;
    private String exchangeReason;
    private Long exchangeRequestedProductId;
    private Boolean canCancel;
    private Boolean canReturn;
    private Boolean canExchange;
    private Boolean canConfirmDelivery;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
