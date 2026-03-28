package com.revshop.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.revshop.config.RazorpayProperties;
import com.revshop.dao.OrderDAO;
import com.revshop.dao.PaymentDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.payment.ConfirmPaymentRequest;
import com.revshop.dto.payment.PaymentResponse;
import com.revshop.dto.payment.ProcessPaymentRequest;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.PaymentMethod;
import com.revshop.entity.PaymentStatus;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ConflictException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;
    private final OrderDAO orderDAO;
    private final UserDAO userDAO;
    private final RazorpayProperties razorpayProperties;

    @Override
    @Transactional
    public PaymentResponse processPayment(String buyerEmail, ProcessPaymentRequest request) {
        log.info("Processing payment for buyerEmail={} and orderId={}", buyerEmail, request.getOrderId());
        User buyer = getValidatedBuyer(buyerEmail);

        com.revshop.entity.Order order = orderDAO.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateBuyerOwnership(order, buyer);
        validateOrderForPayment(order);

        com.revshop.entity.Payment existing = paymentDAO.findByOrderId(order.getId()).orElse(null);
        if (existing != null && existing.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already processed for orderId={}", order.getId());
            throw new ConflictException("Payment already processed for this order");
        }

        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            return processCashOnDelivery(order, buyer, existing, request.getSimulateFailure());
        }

        if (!razorpayProperties.isConfigured()) {
            log.error("Razorpay key id/secret are not configured while processing orderId={}", order.getId());
            throw new BadRequestException("Razorpay key id and key secret must be configured on the server");
        }

        return startRazorpayCheckout(order, buyer, existing, request);
    }

    @Override
    @Transactional
    public PaymentResponse confirmRazorpayPayment(String buyerEmail, ConfirmPaymentRequest request) {
        log.info("Confirming Razorpay payment for buyerEmail={}, orderId={}, razorpayOrderId={}, razorpayPaymentId={}",
                buyerEmail, request.getOrderId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        User buyer = getValidatedBuyer(buyerEmail);

        com.revshop.entity.Order order = orderDAO.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateBuyerOwnership(order, buyer);

        com.revshop.entity.Payment payment = paymentDAO.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already confirmed for orderId={}", request.getOrderId());
            return mapToResponse(payment);
        }

        if (!request.getRazorpayOrderId().equals(payment.getProviderSessionId())) {
            log.warn("Razorpay order id mismatch for orderId={} expected={} actual={}",
                    request.getOrderId(), payment.getProviderSessionId(), request.getRazorpayOrderId());
            throw new BadRequestException("Invalid Razorpay order id for this payment");
        }

        try {
            JSONObject signaturePayload = new JSONObject();
            signaturePayload.put("razorpay_order_id", request.getRazorpayOrderId());
            signaturePayload.put("razorpay_payment_id", request.getRazorpayPaymentId());
            signaturePayload.put("razorpay_signature", request.getRazorpaySignature());

            boolean validSignature = Utils.verifyPaymentSignature(signaturePayload, razorpayProperties.getKeySecret());
            if (!validSignature) {
                log.warn("Razorpay signature verification failed for orderId={}", request.getOrderId());
                throw new BadRequestException("Razorpay payment signature verification failed");
            }

            RazorpayClient razorpayClient = buildRazorpayClient();
            Payment razorpayPayment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
            String paymentStatus = razorpayPayment.get("status");

            payment.setProviderName("RAZORPAY");
            payment.setProviderSessionId(request.getRazorpayOrderId());
            payment.setProviderPaymentIntentId(request.getRazorpayPaymentId());
            payment.setProcessedAt(LocalDateTime.now());

            if ("captured".equalsIgnoreCase(paymentStatus) || "authorized".equalsIgnoreCase(paymentStatus)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setGatewayResponse("Razorpay payment verified successfully.");

                if (payment.getTransactionRef() == null || payment.getTransactionRef().isBlank()) {
                    payment.setTransactionRef(buildTransactionRef());
                }

                order.setStatus(OrderStatus.CONFIRMED);
            } else if ("created".equalsIgnoreCase(paymentStatus)) {
                payment.setStatus(PaymentStatus.INITIATED);
                payment.setGatewayResponse("Razorpay payment is created but not completed yet.");
            } else {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setGatewayResponse("Razorpay payment is not captured yet. Current status: " + paymentStatus);
            }

            paymentDAO.save(payment);
            orderDAO.save(order);
            return mapToResponse(payment);
        } catch (RazorpayException ex) {
            log.error("Unable to verify Razorpay payment for orderId={}", request.getOrderId(), ex);
            throw new BadRequestException("Unable to verify Razorpay payment: " + ex.getMessage());
        }
    }

    private PaymentResponse processCashOnDelivery(
            com.revshop.entity.Order order,
            User buyer,
            com.revshop.entity.Payment existing,
            Boolean simulateFailure) {

        log.info("Processing cash on delivery for orderId={} with simulateFailure={}", order.getId(), simulateFailure);
        PaymentStatus status = resolveMockStatus(order.getPaymentMethod(), simulateFailure);

        com.revshop.entity.Payment payment = existing == null
                ? com.revshop.entity.Payment.builder()
                        .order(order)
                        .buyer(buyer)
                        .paymentMethod(order.getPaymentMethod())
                        .amount(order.getTotalAmount())
                        .active(true)
                        .providerName("COD")
                        .build()
                : existing;

        payment.setStatus(status);
        payment.setTransactionRef(buildTransactionRef());
        payment.setGatewayResponse(buildGatewayResponse(order.getPaymentMethod(), status));
        payment.setProcessedAt(LocalDateTime.now());

        paymentDAO.save(payment);

        if (status == PaymentStatus.SUCCESS || status == PaymentStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        } else if (status == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.PLACED);
        }

        orderDAO.save(order);
        return mapToResponse(payment);
    }

    private PaymentResponse startRazorpayCheckout(
            com.revshop.entity.Order order,
            User buyer,
            com.revshop.entity.Payment existing,
            ProcessPaymentRequest request) {

        log.info("Starting Razorpay checkout for orderId={} and buyerEmail={}", order.getId(), buyer.getEmail());

        try {
            RazorpayClient razorpayClient = buildRazorpayClient();

            JSONObject notes = new JSONObject();
            notes.put("orderId", String.valueOf(order.getId()));
            notes.put("orderNumber", order.getOrderNumber());
            notes.put("buyerEmail", buyer.getEmail());
            notes.put("paymentMethod", order.getPaymentMethod().name());

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", toMinorUnit(order.getTotalAmount()));
            orderRequest.put("currency", razorpayProperties.getCurrency().toUpperCase());
            orderRequest.put("receipt", buildReceipt(order));
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");
            log.info("Razorpay order created for orderId={} with razorpayOrderId={}", order.getId(), razorpayOrderId);

            com.revshop.entity.Payment payment = existing == null
                    ? com.revshop.entity.Payment.builder()
                            .order(order)
                            .buyer(buyer)
                            .paymentMethod(order.getPaymentMethod())
                            .amount(order.getTotalAmount())
                            .active(true)
                            .build()
                    : existing;

            payment.setStatus(PaymentStatus.INITIATED);
            payment.setTransactionRef(buildTransactionRef());
            payment.setGatewayResponse("Razorpay order created. Open Razorpay checkout to complete the payment.");
            payment.setProviderName("RAZORPAY");
            payment.setProviderSessionId(razorpayOrderId);
            payment.setProviderPaymentIntentId(null);
            payment.setProcessedAt(null);

            paymentDAO.save(payment);

            order.setStatus(OrderStatus.PLACED);
            orderDAO.save(order);

            return mapToResponse(payment, buyer, razorpayOrderId, toMinorUnit(order.getTotalAmount()));
        } catch (RazorpayException ex) {
            log.error("Unable to start Razorpay checkout for orderId={}", order.getId(), ex);
            throw new BadRequestException("Unable to start Razorpay checkout: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getBuyerPayments(String buyerEmail) {
        log.info("Fetching payments for buyerEmail={}", buyerEmail);
        User buyer = getValidatedBuyer(buyerEmail);
        return paymentDAO.findByBuyerId(buyer.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(String buyerEmail, Long orderId) {
        log.info("Fetching payment by order for buyerEmail={} and orderId={}", buyerEmail, orderId);
        User buyer = getValidatedBuyer(buyerEmail);

        com.revshop.entity.Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateBuyerOwnership(order, buyer);

        com.revshop.entity.Payment payment = paymentDAO.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));

        return mapToResponse(payment);
    }

    private RazorpayClient buildRazorpayClient() throws RazorpayException {
        return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
    }

    private User getValidatedBuyer(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can access payment APIs");
        }

        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("Buyer account is inactive");
        }

        return user;
    }

    private void validateBuyerOwnership(com.revshop.entity.Order order, User buyer) {
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenOperationException("Order does not belong to buyer");
        }
    }

    private void validateOrderForPayment(com.revshop.entity.Order order) {
        if (!Boolean.TRUE.equals(order.getActive()) || Boolean.TRUE.equals(order.getIsDeleted())) {
            throw new ResourceNotFoundException("Order not found");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Payment cannot be processed for this order status");
        }
    }

    private PaymentStatus resolveMockStatus(PaymentMethod method, Boolean simulateFailure) {
        boolean fail = Boolean.TRUE.equals(simulateFailure);

        if (method == PaymentMethod.CASH_ON_DELIVERY) {
            return PaymentStatus.PENDING;
        }

        return fail ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
    }

    private String buildGatewayResponse(PaymentMethod method, PaymentStatus status) {
        if (method == PaymentMethod.CASH_ON_DELIVERY) {
            return "Cash on delivery selected. Payment will be collected at delivery.";
        }

        if (status == PaymentStatus.SUCCESS) {
            return "Payment authorized successfully.";
        }

        return "Payment gateway declined transaction.";
    }

    private String buildReceipt(com.revshop.entity.Order order) {
        String value = "revshop_" + order.getId() + "_" + order.getOrderNumber();
        return value.length() <= 40 ? value : value.substring(0, 40);
    }

    private String buildTransactionRef() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private long toMinorUnit(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private PaymentResponse mapToResponse(com.revshop.entity.Payment payment) {
        return mapToResponse(payment, payment.getBuyer(), payment.getProviderSessionId(), null);
    }

    private PaymentResponse mapToResponse(
            com.revshop.entity.Payment payment,
            User buyer,
            String gatewayOrderId,
            Long gatewayAmount) {
        String buyerName = resolveBuyerName(buyer);
        String buyerContact = resolveBuyerContact(buyer);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .buyerId(payment.getBuyer().getId())
                .buyerEmail(payment.getBuyer().getEmail())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getStatus())
                .orderStatus(payment.getOrder().getStatus())
                .amount(payment.getAmount())
                .transactionRef(payment.getTransactionRef())
                .gatewayResponse(payment.getGatewayResponse())
                .processedAt(payment.getProcessedAt())
                .checkoutUrl(null)
                .provider(payment.getProviderName())
                .currency(razorpayProperties.getCurrency())
                .gatewayOrderId(gatewayOrderId)
                .gatewayKeyId(shouldExposeGatewayKey(payment) ? razorpayProperties.getKeyId() : null)
                .gatewayAmount(gatewayAmount)
                .buyerName(buyerName == null || buyerName.isBlank() ? payment.getBuyer().getEmail() : buyerName)
                .buyerContact(buyerContact)
                .build();
    }


    private String resolveBuyerName(User buyer) {
        if (buyer == null) {
            return null;
        }
        if (buyer.getBuyerProfile() == null) {
            return buyer.getEmail();
        }
        String firstName = buyer.getBuyerProfile().getFirstName() == null ? "" : buyer.getBuyerProfile().getFirstName();
        String lastName = buyer.getBuyerProfile().getLastName() == null ? "" : buyer.getBuyerProfile().getLastName();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? buyer.getEmail() : fullName;
    }

    private String resolveBuyerContact(User buyer) {
        if (buyer == null || buyer.getBuyerProfile() == null) {
            return null;
        }
        return buyer.getBuyerProfile().getPhone();
    }

    private boolean shouldExposeGatewayKey(com.revshop.entity.Payment payment) {
        return payment.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY
                && payment.getStatus() == PaymentStatus.INITIATED
                && "RAZORPAY".equalsIgnoreCase(payment.getProviderName());
    }
}
