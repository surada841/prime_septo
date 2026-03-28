package com.revshop.dao;

import com.revshop.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentDAO {

    Payment save(Payment payment);

    Optional<Payment> findById(Long paymentId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByBuyerId(Long buyerId);
}
