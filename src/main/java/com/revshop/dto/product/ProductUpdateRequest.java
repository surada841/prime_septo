package com.revshop.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must be <= 150 chars")
    private String name;

    @Size(max = 2000, message = "Description must be <= 2000 chars")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "MRP must be greater than 0")
    private BigDecimal mrpPrice;

    @DecimalMin(value = "0.01", message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;
}
