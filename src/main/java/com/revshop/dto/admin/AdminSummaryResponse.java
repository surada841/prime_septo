package com.revshop.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSummaryResponse {

    private long totalUsers;
    private long totalBuyers;
    private long totalSellers;
    private long activeUsers;
    private long deletedUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalPayments;
}
