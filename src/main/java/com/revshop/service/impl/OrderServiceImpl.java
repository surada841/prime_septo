package com.revshop.service.impl;

import com.revshop.dao.CartDAO;
import com.revshop.dao.CartItemDAO;
import com.revshop.dao.OrderDAO;
import com.revshop.dao.OrderItemDAO;
import com.revshop.dao.PaymentDAO;
import com.revshop.dao.ProductDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.order.OrderItemResponse;
import com.revshop.dto.order.OrderResponse;
import com.revshop.dto.order.PlaceOrderRequest;
import com.revshop.entity.Cart;
import com.revshop.entity.CartItem;
import com.revshop.entity.NotificationType;
import com.revshop.entity.Order;
import com.revshop.entity.OrderItem;
import com.revshop.entity.OrderStatus;
import com.revshop.entity.Payment;
import com.revshop.entity.PaymentMethod;
import com.revshop.entity.PaymentStatus;
import com.revshop.entity.Product;
import com.revshop.entity.ProductStatus;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.NotificationService;
import com.revshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final CartDAO cartDAO;
    private final CartItemDAO cartItemDAO;
    private final PaymentDAO paymentDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponse placeOrder(String buyerEmail, PlaceOrderRequest request) {
        User buyer = getValidatedBuyer(buyerEmail);
        Cart cart = cartDAO.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> cartItems = cartItemDAO.findActiveByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productDAO.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));

            if (!Boolean.TRUE.equals(product.getActive()) || Boolean.TRUE.equals(product.getIsDeleted())) {
                throw new BadRequestException("Product is not available: " + product.getName());
            }
            if (cartItem.getQuantity() > product.getStock()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .status(OrderStatus.PLACED)
                .paymentMethod(request.getPaymentMethod())
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .totalAmount(totalAmount)
                .active(true)
                .build();
        orderDAO.save(order);

        List<OrderItem> createdOrderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productDAO.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));

            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .seller(product.getSeller())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .active(true)
                    .build();
            OrderItem savedOrderItem = orderItemDAO.save(orderItem);
            createdOrderItems.add(savedOrderItem);

            int previousStock = product.getStock();
            int updatedStock = product.getStock() - cartItem.getQuantity();
            product.setStock(updatedStock);
            product.setInStock(updatedStock > 0);
            if (updatedStock <= 0) {
                product.setStatus(com.revshop.entity.ProductStatus.OUT_OF_STOCK);
            }
            productDAO.save(product);
            sendLowStockNotificationIfNeeded(product, previousStock, updatedStock);

            cartItem.setActive(false);
            cartItem.setIsDeleted(true);
            cartItemDAO.save(cartItem);
        }

        sendOrderNotifications(order, createdOrderItems);
        return buildOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getBuyerOrders(String buyerEmail) {
        User buyer = getValidatedBuyer(buyerEmail);
        return orderDAO.findByBuyerId(buyer.getId())
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getBuyerOrderById(String buyerEmail, Long orderId) {
        User buyer = getValidatedBuyer(buyerEmail);
        Order order = getOwnedActiveOrder(buyer, orderId);
        return buildOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getSellerOrders(String sellerEmail) {
        User seller = getValidatedSeller(sellerEmail);
        return orderDAO.findBySellerEmail(seller.getEmail())
                .stream()
                .map(order -> buildOrderResponseForSeller(order, seller.getEmail()))
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancelBuyerOrder(String buyerEmail, Long orderId, String reason) {
        User buyer = getValidatedBuyer(buyerEmail);
        Order order = getOwnedActiveOrder(buyer, orderId);
        if (!canCancel(order.getStatus())) {
            throw new BadRequestException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(normalizeReason(reason, "Cancelled by buyer"));
        order.setReturnReason(null);
        order.setExchangeReason(null);
        order.setExchangeRequestedProductId(null);
        restoreOrderStock(order);
        orderDAO.save(order);

        sendOrderCancelledNotifications(order, orderItemDAO.findByOrderId(order.getId()));
        return buildOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(String buyerEmail, Long orderId, String reason) {
        User buyer = getValidatedBuyer(buyerEmail);
        Order order = getOwnedActiveOrder(buyer, orderId);
        if (!canReturn(order.getStatus())) {
            throw new BadRequestException("Return can be requested only for delivered orders");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setCancelReason(null);
        order.setReturnReason(normalizeReason(reason, "Return requested by buyer"));
        order.setExchangeReason(null);
        order.setExchangeRequestedProductId(null);
        orderDAO.save(order);

        sendReturnRequestedNotifications(order, orderItemDAO.findByOrderId(order.getId()));
        return buildOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse requestExchange(String buyerEmail, Long orderId, String reason, Long exchangeProductId) {
        User buyer = getValidatedBuyer(buyerEmail);
        Order order = getOwnedActiveOrder(buyer, orderId);
        if (!canExchange(order.getStatus())) {
            throw new BadRequestException("Exchange can be requested only for delivered orders");
        }
        if (exchangeProductId != null) {
            Product target = productDAO.findById(exchangeProductId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exchange target product not found"));
            if (!Boolean.TRUE.equals(target.getActive()) || Boolean.TRUE.equals(target.getIsDeleted())) {
                throw new BadRequestException("Exchange target product is not active");
            }
            if (target.getStock() <= 0) {
                throw new BadRequestException("Exchange target product is out of stock");
            }
        }

        order.setStatus(OrderStatus.EXCHANGE_REQUESTED);
        order.setCancelReason(null);
        order.setExchangeReason(normalizeReason(reason, "Exchange requested by buyer"));
        order.setExchangeRequestedProductId(exchangeProductId);
        order.setReturnReason(null);
        orderDAO.save(order);

        sendExchangeRequestedNotifications(order, orderItemDAO.findByOrderId(order.getId()));
        return buildOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markOrderShippedBySeller(String sellerEmail, Long orderId) {
        User seller = getValidatedSeller(sellerEmail);
        Order order = getSellerVisibleOrder(seller, orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed orders can be marked as shipped");
        }

        order.setStatus(OrderStatus.SHIPPED);
        orderDAO.save(order);

        List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getId());
        sendOrderShippedNotifications(order, orderItems);
        return buildOrderResponseForSeller(order, seller.getEmail());
    }

    @Override
    @Transactional
    public OrderResponse markOrderDeliveredBySeller(String sellerEmail, Long orderId) {
        User seller = getValidatedSeller(sellerEmail);
        Order order = getSellerVisibleOrder(seller, orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Only shipped orders can be marked as delivered");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderDAO.save(order);
        boolean codPaymentCollected = settleCodPaymentIfPending(order);

        List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getId());
        sendOrderDeliveredNotifications(order, orderItems, false, codPaymentCollected);
        return buildOrderResponseForSeller(order, seller.getEmail());
    }

    @Override
    @Transactional
    public OrderResponse confirmOrderDeliveredByBuyer(String buyerEmail, Long orderId) {
        User buyer = getValidatedBuyer(buyerEmail);
        Order order = getOwnedActiveOrder(buyer, orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Only shipped orders can be confirmed as delivered");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderDAO.save(order);
        boolean codPaymentCollected = settleCodPaymentIfPending(order);

        List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getId());
        sendOrderDeliveredNotifications(order, orderItems, true, codPaymentCollected);
        return buildOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completeReturnBySeller(String sellerEmail, Long orderId) {
        User seller = getValidatedSeller(sellerEmail);
        Order order = getSellerVisibleOrder(seller, orderId);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BadRequestException("Only return requested orders can be marked as returned");
        }

        order.setStatus(OrderStatus.RETURNED);
        orderDAO.save(order);
        restoreOrderStock(order);
        boolean refunded = refundPaymentIfApplicable(order);

        List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getId());
        sendReturnCompletedNotifications(order, orderItems, refunded);
        return buildOrderResponseForSeller(order, seller.getEmail());
    }

    @Override
    @Transactional
    public OrderResponse completeExchangeBySeller(String sellerEmail, Long orderId) {
        User seller = getValidatedSeller(sellerEmail);
        Order order = getSellerVisibleOrder(seller, orderId);
        if (order.getStatus() != OrderStatus.EXCHANGE_REQUESTED) {
            throw new BadRequestException("Only exchange requested orders can be marked as exchanged");
        }

        if (order.getExchangeRequestedProductId() != null) {
            Product target = productDAO.findById(order.getExchangeRequestedProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exchange target product not found"));
            if (!Boolean.TRUE.equals(target.getActive()) || Boolean.TRUE.equals(target.getIsDeleted())) {
                throw new BadRequestException("Exchange target product is not active");
            }
            if (target.getStock() <= 0) {
                throw new BadRequestException("Exchange target product is out of stock");
            }
        }

        order.setStatus(OrderStatus.EXCHANGED);
        orderDAO.save(order);

        List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getId());
        sendExchangeCompletedNotifications(order, orderItems);
        return buildOrderResponseForSeller(order, seller.getEmail());
    }

    private User getValidatedBuyer(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.BUYER) {
            throw new ForbiddenOperationException("Only buyer can place/view buyer orders");
        }
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("Buyer account is inactive");
        }
        return user;
    }

    private User getValidatedSeller(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only seller can view seller orders");
        }
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ForbiddenOperationException("Seller account is inactive");
        }
        return user;
    }

    private Order getOwnedActiveOrder(User buyer, Long orderId) {
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ForbiddenOperationException("Order does not belong to buyer");
        }
        if (!Boolean.TRUE.equals(order.getActive()) || Boolean.TRUE.equals(order.getIsDeleted())) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    private Order getSellerVisibleOrder(User seller, Long orderId) {
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        List<OrderItem> items = orderItemDAO.findByOrderId(order.getId());
        boolean sellerOwnsAnyItem = items.stream()
                .anyMatch(item -> item.getSeller() != null && seller.getId().equals(item.getSeller().getId()));
        if (!sellerOwnsAnyItem) {
            throw new ForbiddenOperationException("Order does not belong to seller");
        }
        return order;
    }

    private boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED;
    }

    private boolean canReturn(OrderStatus status) {
        return status == OrderStatus.DELIVERED;
    }

    private boolean canExchange(OrderStatus status) {
        return status == OrderStatus.DELIVERED;
    }

    private boolean canConfirmDelivery(OrderStatus status) {
        return status == OrderStatus.SHIPPED;
    }

    private PaymentStatus getPaymentStatus(Order order) {
        return paymentDAO.findByOrderId(order.getId())
                .map(Payment::getStatus)
                .orElse(null);
    }

    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItemResponse> items = orderItemDAO.findByOrderId(order.getId())
                .stream()
                .map(this::mapOrderItem)
                .toList();
        PaymentStatus paymentStatus = getPaymentStatus(order);

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .buyerId(order.getBuyer().getId())
                .buyerEmail(order.getBuyer().getEmail())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(paymentStatus)
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .totalAmount(order.getTotalAmount())
                .cancelReason(order.getCancelReason())
                .returnReason(order.getReturnReason())
                .exchangeReason(order.getExchangeReason())
                .exchangeRequestedProductId(order.getExchangeRequestedProductId())
                .canCancel(canCancel(order.getStatus()))
                .canReturn(canReturn(order.getStatus()))
                .canExchange(canExchange(order.getStatus()))
                .canConfirmDelivery(canConfirmDelivery(order.getStatus()))
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private OrderResponse buildOrderResponseForSeller(Order order, String sellerEmail) {
        List<OrderItemResponse> items = orderItemDAO.findByOrderId(order.getId())
                .stream()
                .filter(item -> item.getSeller().getEmail().equals(sellerEmail))
                .map(this::mapOrderItem)
                .toList();
        PaymentStatus paymentStatus = getPaymentStatus(order);

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .buyerId(order.getBuyer().getId())
                .buyerEmail(order.getBuyer().getEmail())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(paymentStatus)
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .totalAmount(items.stream().map(OrderItemResponse::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .cancelReason(order.getCancelReason())
                .returnReason(order.getReturnReason())
                .exchangeReason(order.getExchangeReason())
                .exchangeRequestedProductId(order.getExchangeRequestedProductId())
                .canCancel(false)
                .canReturn(false)
                .canExchange(false)
                .canConfirmDelivery(false)
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private OrderItemResponse mapOrderItem(OrderItem item) {
        return OrderItemResponse.builder()
                .orderItemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .sellerId(item.getSeller().getId())
                .sellerEmail(item.getSeller().getEmail())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private void restoreOrderStock(Order order) {
        List<OrderItem> items = orderItemDAO.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Product product = item.getProduct();
            int updatedStock = product.getStock() + item.getQuantity();
            product.setStock(updatedStock);
            product.setInStock(updatedStock > 0);
            if (updatedStock > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            productDAO.save(product);
        }
    }

    private boolean settleCodPaymentIfPending(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            return false;
        }
        Payment payment = paymentDAO.findByOrderId(order.getId()).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("Mock COD payment collected on delivery.");
        paymentDAO.save(payment);
        return true;
    }

    private boolean refundPaymentIfApplicable(Order order) {
        Payment payment = paymentDAO.findByOrderId(order.getId()).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            return false;
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("Mock refund processed to buyer after return completion.");
        paymentDAO.save(payment);
        return true;
    }

    private String codPaymentNoteForSeller(Order order, PaymentStatus paymentStatus, boolean codPaymentCollected) {
        if (order.getPaymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
            return "";
        }
        if (codPaymentCollected || paymentStatus == PaymentStatus.SUCCESS) {
            return " COD payment has been collected.";
        }
        return " COD payment is pending.";
    }

    private String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        String normalized = reason.trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private void sendOrderNotifications(Order order, List<OrderItem> orderItems) {
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.ORDER_PLACED,
                "Order placed successfully",
                "Your order " + order.getOrderNumber() + " has been placed.",
                "ORDER",
                order.getId()
        );

        Map<Long, BigDecimal> sellerAmounts = new HashMap<>();
        Map<Long, Integer> sellerItemCounts = new HashMap<>();

        for (OrderItem orderItem : orderItems) {
            Long sellerId = orderItem.getSeller().getId();
            sellerAmounts.merge(sellerId, orderItem.getLineTotal(), BigDecimal::add);
            sellerItemCounts.merge(sellerId, orderItem.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, BigDecimal> entry : sellerAmounts.entrySet()) {
            Long sellerId = entry.getKey();
            BigDecimal sellerAmount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            Integer itemCount = sellerItemCounts.getOrDefault(sellerId, 0);

            notificationService.createNotification(
                    sellerId,
                    NotificationType.ORDER_RECEIVED,
                    "New order received",
                    "Order " + order.getOrderNumber()
                            + " from " + order.getBuyer().getEmail()
                            + " | Items: " + itemCount
                            + " | Amount: INR " + sellerAmount.toPlainString(),
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendOrderCancelledNotifications(Order order, List<OrderItem> orderItems) {
        String reason = order.getCancelReason() == null ? "-" : order.getCancelReason();
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.ORDER_CANCELLED,
                "Order cancelled",
                "Order " + order.getOrderNumber() + " has been cancelled. Reason: " + reason,
                "ORDER",
                order.getId()
        );

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.ORDER_CANCELLED,
                    "Buyer cancelled order",
                    "Order " + order.getOrderNumber() + " was cancelled by buyer.",
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendOrderShippedNotifications(Order order, List<OrderItem> orderItems) {
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.ORDER_SHIPPED,
                "Order shipped",
                "Your order " + order.getOrderNumber() + " has been shipped.",
                "ORDER",
                order.getId()
        );
        String codPaymentNote = order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                ? " COD payment is pending until delivery confirmation."
                : "";

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.ORDER_SHIPPED,
                    "Order marked as shipped",
                    "Order " + order.getOrderNumber() + " is now marked as shipped." + codPaymentNote,
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendOrderDeliveredNotifications(
            Order order,
            List<OrderItem> orderItems,
            boolean confirmedByBuyer,
            boolean codPaymentCollected
    ) {
        String buyerTitle = confirmedByBuyer ? "Delivery confirmed" : "Order delivered";
        String buyerMessage = confirmedByBuyer
                ? "You confirmed delivery for order " + order.getOrderNumber() + "."
                : "Your order " + order.getOrderNumber() + " has been delivered.";
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.ORDER_DELIVERED,
                buyerTitle,
                buyerMessage,
                "ORDER",
                order.getId()
        );
        PaymentStatus paymentStatus = getPaymentStatus(order);
        String codPaymentNote = codPaymentNoteForSeller(order, paymentStatus, codPaymentCollected);
        String sellerTitle = confirmedByBuyer ? "Buyer confirmed delivery" : "Order marked as delivered";
        String sellerMessage = confirmedByBuyer
                ? "Buyer confirmed delivery for order " + order.getOrderNumber() + "." + codPaymentNote
                : "Order " + order.getOrderNumber() + " is now marked as delivered." + codPaymentNote;

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.ORDER_DELIVERED,
                    sellerTitle,
                    sellerMessage,
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendReturnRequestedNotifications(Order order, List<OrderItem> orderItems) {
        String reason = order.getReturnReason() == null ? "-" : order.getReturnReason();
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.RETURN_REQUESTED,
                "Return requested",
                "Return request submitted for order " + order.getOrderNumber() + ". Reason: " + reason,
                "ORDER",
                order.getId()
        );

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.RETURN_REQUESTED,
                    "Return request received",
                    "Buyer requested return for order " + order.getOrderNumber() + ".",
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendReturnCompletedNotifications(Order order, List<OrderItem> orderItems, boolean refunded) {
        String refundNoteBuyer = refunded
                ? " Payment has been refunded to buyer."
                : " Payment refund is not applicable for this order.";
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.RETURN_COMPLETED,
                "Return completed",
                "Return for order " + order.getOrderNumber() + " has been completed." + refundNoteBuyer,
                "ORDER",
                order.getId()
        );

        String refundNoteSeller = refunded
                ? " Payment has been refunded to buyer."
                : " No payment refund was required.";
        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.RETURN_COMPLETED,
                    "Return completed",
                    "Return for order " + order.getOrderNumber() + " has been completed." + refundNoteSeller,
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendExchangeRequestedNotifications(Order order, List<OrderItem> orderItems) {
        String reason = order.getExchangeReason() == null ? "-" : order.getExchangeReason();
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.EXCHANGE_REQUESTED,
                "Exchange requested",
                "Exchange request submitted for order " + order.getOrderNumber() + ". Reason: " + reason,
                "ORDER",
                order.getId()
        );

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.EXCHANGE_REQUESTED,
                    "Exchange request received",
                    "Buyer requested exchange for order " + order.getOrderNumber() + ".",
                    "ORDER",
                    order.getId()
            );
        }
    }

    private void sendExchangeCompletedNotifications(Order order, List<OrderItem> orderItems) {
        notificationService.createNotification(
                order.getBuyer().getId(),
                NotificationType.EXCHANGE_COMPLETED,
                "Exchange completed",
                "Exchange for order " + order.getOrderNumber() + " has been completed.",
                "ORDER",
                order.getId()
        );

        for (Long sellerId : getOrderSellerIds(orderItems)) {
            notificationService.createNotification(
                    sellerId,
                    NotificationType.EXCHANGE_COMPLETED,
                    "Exchange completed",
                    "Exchange for order " + order.getOrderNumber() + " has been completed.",
                    "ORDER",
                    order.getId()
            );
        }
    }

    private Set<Long> getOrderSellerIds(List<OrderItem> orderItems) {
        Set<Long> sellerIds = new LinkedHashSet<>();
        for (OrderItem item : orderItems) {
            if (item.getSeller() != null && item.getSeller().getId() != null) {
                sellerIds.add(item.getSeller().getId());
            }
        }
        return sellerIds;
    }

    private void sendLowStockNotificationIfNeeded(Product product, int previousStock, int updatedStock) {
        int threshold = product.getLowStockThreshold() == null ? 5 : product.getLowStockThreshold();
        if (previousStock > threshold && updatedStock <= threshold) {
            notificationService.createNotification(
                    product.getSeller().getId(),
                    NotificationType.LOW_STOCK_ALERT,
                    "Low stock alert",
                    "Product '" + product.getName() + "' is low on stock. Current stock: " + updatedStock + ".",
                    "PRODUCT",
                    product.getId()
            );
        }
    }

    private String generateOrderNumber() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "ORD-" + token;
    }
}
