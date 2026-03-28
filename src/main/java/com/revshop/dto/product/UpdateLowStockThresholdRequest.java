package com.revshop.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLowStockThresholdRequest {

    @NotNull(message = "Low stock threshold is required")
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;
}
