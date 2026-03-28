package com.revshop.dto.seller;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SellerDashboardOverviewResponse {

    private Long totalProducts;
    private Long activeProducts;
    private Long lowStockProducts;
    private Long totalOrders;
    private Long pendingOrders;
    private Long totalUnitsSold;
    private BigDecimal grossRevenue;
}
