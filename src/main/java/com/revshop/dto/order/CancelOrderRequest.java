package com.revshop.dto.order;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
