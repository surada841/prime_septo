package com.revshop.service.impl;

import com.revshop.dao.OrderItemDAO;
import com.revshop.dao.ProductDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.seller.SellerDashboardOverviewResponse;
import com.revshop.dto.seller.SellerDashboardResponse;
import com.revshop.dto.seller.SellerRecentOrderResponse;
import com.revshop.dto.seller.SellerTopProductResponse;
import com.revshop.entity.OrderItem;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerDashboardServiceImpl implements SellerDashboardService {

    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final OrderItemDAO orderItemDAO;

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(String sellerEmail, int recentLimit, int topLimit, int lowStockThreshold) {
        validateSeller(sellerEmail);

        long totalProducts = productDAO.countBySellerEmail(sellerEmail);
        long activeProducts = productDAO.countActiveBySellerEmail(sellerEmail);
        long lowStockProducts = productDAO.countLowStockBySellerEmail(sellerEmail, Math.max(0, lowStockThreshold));
        long totalOrders = orderItemDAO.countDistinctOrdersBySellerEmail(sellerEmail);
        long pendingOrders = orderItemDAO.countDistinctPendingOrdersBySellerEmail(sellerEmail);
        long totalUnitsSold = orderItemDAO.sumQuantityBySellerEmail(sellerEmail);
        BigDecimal grossRevenue = orderItemDAO.sumRevenueBySellerEmail(sellerEmail);

        SellerDashboardOverviewResponse overview = SellerDashboardOverviewResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalUnitsSold(totalUnitsSold)
                .grossRevenue(grossRevenue)
                .build();

        List<SellerRecentOrderResponse> recentOrders = buildRecentOrders(sellerEmail, recentLimit);
        List<SellerTopProductResponse> topProducts = buildTopProducts(sellerEmail, topLimit);

        return SellerDashboardResponse.builder()
                .overview(overview)
                .recentOrders(recentOrders)
                .topProducts(topProducts)
                .build();
    }

    private void validateSeller(String sellerEmail) {
        User seller = userDAO.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only seller can access seller dashboard");
        }
        if (!Boolean.TRUE.equals(seller.getActive()) || Boolean.TRUE.equals(seller.getIsDeleted())) {
            throw new ForbiddenOperationException("Seller account is inactive");
        }
    }

    private List<SellerRecentOrderResponse> buildRecentOrders(String sellerEmail, int recentLimit) {
        List<OrderItem> items = orderItemDAO.findBySellerEmail(sellerEmail);
        Map<Long, SellerRecentOrderAccumulator> map = new LinkedHashMap<>();

        for (OrderItem item : items) {
            Long orderId = item.getOrder().getId();
            SellerRecentOrderAccumulator acc = map.computeIfAbsent(orderId, id -> new SellerRecentOrderAccumulator(
                    id,
                    item.getOrder().getOrderNumber(),
                    item.getOrder().getBuyer().getEmail(),
                    item.getOrder().getStatus(),
                    item.getOrder().getCreatedAt(),
                    0,
                    BigDecimal.ZERO
            ));
            acc.itemCount += item.getQuantity();
            acc.orderAmountForSeller = acc.orderAmountForSeller.add(item.getLineTotal());
        }

        int limit = Math.max(1, recentLimit);
        List<SellerRecentOrderResponse> response = new ArrayList<>();
        int i = 0;
        for (SellerRecentOrderAccumulator acc : map.values()) {
            if (i >= limit) {
                break;
            }
            response.add(SellerRecentOrderResponse.builder()
                    .orderId(acc.orderId)
                    .orderNumber(acc.orderNumber)
                    .buyerEmail(acc.buyerEmail)
                    .orderStatus(acc.orderStatus)
                    .itemCount(acc.itemCount)
                    .orderAmountForSeller(acc.orderAmountForSeller)
                    .orderedAt(acc.orderedAt)
                    .build());
            i++;
        }
        return response;
    }

    private List<SellerTopProductResponse> buildTopProducts(String sellerEmail, int topLimit) {
        int limit = Math.max(1, topLimit);
        return orderItemDAO.findTopProductsBySellerEmail(sellerEmail, limit)
                .stream()
                .map(row -> SellerTopProductResponse.builder()
                        .productId((Long) row[0])
                        .productName((String) row[1])
                        .currentStock((Integer) row[2])
                        .unitsSold(((Number) row[3]).longValue())
                        .revenue((BigDecimal) row[4])
                        .build())
                .toList();
    }

    private static class SellerRecentOrderAccumulator {
        Long orderId;
        String orderNumber;
        String buyerEmail;
        com.revshop.entity.OrderStatus orderStatus;
        java.time.LocalDateTime orderedAt;
        int itemCount;
        BigDecimal orderAmountForSeller;

        SellerRecentOrderAccumulator(
                Long orderId,
                String orderNumber,
                String buyerEmail,
                com.revshop.entity.OrderStatus orderStatus,
                java.time.LocalDateTime orderedAt,
                int itemCount,
                BigDecimal orderAmountForSeller
        ) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.buyerEmail = buyerEmail;
            this.orderStatus = orderStatus;
            this.orderedAt = orderedAt;
            this.itemCount = itemCount;
            this.orderAmountForSeller = orderAmountForSeller;
        }
    }
}
