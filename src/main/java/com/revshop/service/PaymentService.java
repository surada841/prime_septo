package com.revshop.service;

import com.revshop.dto.payment.ConfirmPaymentRequest;
import com.revshop.dto.payment.PaymentResponse;
import com.revshop.dto.payment.ProcessPaymentRequest;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(String buyerEmail, ProcessPaymentRequest request);

    PaymentResponse confirmRazorpayPayment(String buyerEmail, ConfirmPaymentRequest request);

    List<PaymentResponse> getBuyerPayments(String buyerEmail);

    PaymentResponse getPaymentByOrder(String buyerEmail, Long orderId);
}
