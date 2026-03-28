package com.revshop.dto.seller;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SellerDashboardResponse {

    private SellerDashboardOverviewResponse overview;
    private List<SellerRecentOrderResponse> recentOrders;
    private List<SellerTopProductResponse> topProducts;
}
