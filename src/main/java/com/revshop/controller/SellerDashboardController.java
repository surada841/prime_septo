package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.seller.SellerDashboardResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/dashboard")
@RequiredArgsConstructor
@Tag(name = "Seller Dashboard", description = "Seller KPIs, recent orders, and top products")
@SecurityRequirement(name = "bearerAuth")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> dashboard(
            Authentication auth,
            @RequestParam(defaultValue = "5") int recentLimit,
            @RequestParam(defaultValue = "5") int topLimit,
            @RequestParam(defaultValue = "5") int lowStockThreshold
    ) {
        SellerDashboardResponse response = sellerDashboardService.getDashboard(
                auth.getName(),
                recentLimit,
                topLimit,
                lowStockThreshold
        );
        return ResponseEntity.ok(ApiResponse.success("Seller dashboard fetched", response));
    }
}
