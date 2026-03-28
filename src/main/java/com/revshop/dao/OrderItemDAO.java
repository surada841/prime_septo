package com.revshop.dao;

import com.revshop.entity.OrderItem;

import java.util.List;

public interface OrderItemDAO {

    OrderItem save(OrderItem orderItem);

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findBySellerEmail(String sellerEmail);

    long countDistinctOrdersBySellerEmail(String sellerEmail);

    long countDistinctPendingOrdersBySellerEmail(String sellerEmail);

    long sumQuantityBySellerEmail(String sellerEmail);

    java.math.BigDecimal sumRevenueBySellerEmail(String sellerEmail);

    List<Object[]> findTopProductsBySellerEmail(String sellerEmail, int limit);
}
