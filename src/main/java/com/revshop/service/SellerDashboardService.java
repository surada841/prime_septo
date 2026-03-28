package com.revshop.service;

import com.revshop.dto.seller.SellerDashboardResponse;

public interface SellerDashboardService {

    SellerDashboardResponse getDashboard(String sellerEmail, int recentLimit, int topLimit, int lowStockThreshold);
}
