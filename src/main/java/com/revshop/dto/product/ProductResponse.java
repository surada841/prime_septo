package com.revshop.dto.product;

import com.revshop.entity.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal mrpPrice;
    private BigDecimal discountedPrice;
    private Integer stock;
    private Integer lowStockThreshold;
    private Boolean inStock;
    private Boolean active;
    private ProductStatus status;

    private Long categoryId;
    private String categoryName;

    private Long sellerId;
    private String sellerEmail;
    private List<String> imageUrls;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
