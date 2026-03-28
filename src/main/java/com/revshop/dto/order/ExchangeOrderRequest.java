package com.revshop.dto.order;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeOrderRequest {

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    private Long exchangeProductId;
}
