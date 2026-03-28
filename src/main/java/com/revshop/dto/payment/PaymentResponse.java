package com.revshop.dto.payment;

import com.revshop.entity.OrderStatus;
import com.revshop.entity.PaymentMethod;
import com.revshop.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private Long buyerId;
    private String buyerEmail;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private BigDecimal amount;
    private String transactionRef;
    private String gatewayResponse;
    private LocalDateTime processedAt;
    private String checkoutUrl;
    private String provider;
    private String currency;
    private String gatewayOrderId;
    private String gatewayKeyId;
    private Long gatewayAmount;
    private String buyerName;
    private String buyerContact;
}
