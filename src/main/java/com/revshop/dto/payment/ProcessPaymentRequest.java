package com.revshop.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPaymentRequest {

    @NotNull(message = "Order id is required")
    private Long orderId;

    private Boolean simulateFailure = false;

    private String successUrl;

    private String cancelUrl;
}
