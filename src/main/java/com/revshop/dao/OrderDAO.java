package com.revshop.dao;

import com.revshop.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderDAO {

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    List<Order> findByBuyerId(Long buyerId);

    List<Order> findBySellerEmail(String sellerEmail);
}
