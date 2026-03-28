package com.revshop.dto.order;

import com.revshop.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address must be <= 500 chars")
    private String shippingAddress;

    @NotBlank(message = "Billing address is required")
    @Size(max = 500, message = "Billing address must be <= 500 chars")
    private String billingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
