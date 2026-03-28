package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.payment.ConfirmPaymentRequest;
import com.revshop.dto.payment.PaymentResponse;
import com.revshop.dto.payment.ProcessPaymentRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment processing and payment tracking APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            Authentication auth
    ) {
        PaymentResponse response = paymentService.processPayment(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request,
            Authentication auth
    ) {
        PaymentResponse response = paymentService.confirmRazorpayPayment(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment status verified", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> myPayments(Authentication auth) {
        List<PaymentResponse> response = paymentService.getBuyerPayments(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Buyer payments fetched", response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentByOrder(
            @PathVariable Long orderId,
            Authentication auth
    ) {
        PaymentResponse response = paymentService.getPaymentByOrder(auth.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched by order", response));
    }
}
