"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "cart",
        title: "Cart and Checkout",
        subtitle: "Checkout with cash on delivery or Razorpay payment in INR."
    });

    const cartTableBody = document.getElementById("cartTableBody");
    const emptyCartState = document.getElementById("emptyCartState");
    const summaryItems = document.getElementById("summaryItems");
    const summaryTotal = document.getElementById("summaryTotal");
    const clearCartBtn = document.getElementById("clearCartBtn");

    const checkoutForm = document.getElementById("checkoutForm");
    const shippingAddressInput = document.getElementById("shippingAddressInput");
    const billingAddressInput = document.getElementById("billingAddressInput");
    const paymentMethodOptions = document.querySelectorAll("input[name=\"paymentMethodOption\"]");
    const paymentMethodCards = document.querySelectorAll(".payment-method-card");

    let cartState = null;

    function renderCart(cart) {
        cartState = cart;
        const items = cart.items || [];
        cartTableBody.innerHTML = "";

        summaryItems.textContent = String(cart.totalItems || 0);
        summaryTotal.textContent = app.formatCurrency(cart.grandTotal || 0);

        if (items.length === 0) {
            emptyCartState.classList.remove("d-none");
            return;
        }

        emptyCartState.classList.add("d-none");
        items.forEach((item) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>
                    <div class="fw-semibold">${app.escapeHtml(item.productName)}</div>
                </td>
                <td class="text-end">${app.formatCurrency(item.unitPrice)}</td>
                <td class="text-center">
                    <div class="d-inline-flex align-items-center gap-2">
                        <input type="number" min="1" class="form-control market-input quantity-input" style="width: 82px;"
                               data-item-id="${item.itemId}" value="${item.quantity}">
                        <button class="btn btn-sm btn-outline-primary update-qty-btn" data-item-id="${item.itemId}">Update</button>
                    </div>
                </td>
                <td class="text-end">${app.formatCurrency(item.lineTotal)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger remove-item-btn" data-item-id="${item.itemId}">Remove</button>
                </td>
            `;
            cartTableBody.appendChild(row);
        });
    }

    async function loadCart() {
        try {
            const cart = await app.api("/cart");
            renderCart(cart);
        } catch (error) {
            app.showToast(error.message || "Failed to load cart", "error");
        }
    }

    async function updateItemQuantity(itemId, quantity) {
        try {
            await app.api(`/cart/items/${itemId}`, {
                method: "PUT",
                body: { quantity }
            });
            app.showToast("Quantity updated", "success");
            await loadCart();
        } catch (error) {
            app.showToast(error.message || "Failed to update quantity", "error");
        }
    }

    async function removeItem(itemId) {
        try {
            await app.api(`/cart/items/${itemId}`, { method: "DELETE" });
            app.showToast("Item removed", "success");
            await loadCart();
        } catch (error) {
            app.showToast(error.message || "Failed to remove item", "error");
        }
    }

    async function clearCart() {
        try {
            await app.api("/cart/clear", { method: "DELETE" });
            app.showToast("Cart cleared", "success");
            await loadCart();
        } catch (error) {
            app.showToast(error.message || "Failed to clear cart", "error");
        }
    }


    function openRazorpayCheckout(order, paymentInfo) {
        if (typeof window.Razorpay !== "function") {
            app.showToast("Razorpay checkout script did not load", "error");
            return;
        }

        const options = {
            key: paymentInfo.gatewayKeyId,
            amount: paymentInfo.gatewayAmount,
            currency: paymentInfo.currency || "INR",
            name: "RevShop",
            description: `Order ${order.orderNumber}`,
            order_id: paymentInfo.gatewayOrderId,
            handler: async function (response) {
                try {
                    const payment = await app.api("/payments/confirm", {
                        method: "POST",
                        body: {
                            orderId: order.orderId,
                            razorpayOrderId: response.razorpay_order_id,
                            razorpayPaymentId: response.razorpay_payment_id,
                            razorpaySignature: response.razorpay_signature
                        }
                    });

                    app.showToast(`Payment ${payment.paymentStatus}. Redirecting to order status...`, "success");
                    window.location.href = `${window.location.origin}/buyer/payment-success?orderId=${encodeURIComponent(order.orderId)}&paymentId=${encodeURIComponent(response.razorpay_payment_id)}`;
                } catch (confirmError) {
                    app.showToast(confirmError.message || "Payment verification failed", "error");
                }
            },
            prefill: {
                name: paymentInfo.buyerName || "",
                email: paymentInfo.buyerEmail || "",
                contact: paymentInfo.buyerContact || ""
            },
            theme: {
                color: "#0d6efd"
            },
            modal: {
                ondismiss: function () {
                    app.showToast("Payment popup closed", "error");
                }
            }
        };

        const razorpay = new window.Razorpay(options);
        razorpay.on("payment.failed", function (response) {
            const reason = response?.error?.description || "Payment failed";
            app.showToast(reason, "error");
            window.location.href = `${window.location.origin}/buyer/payment-cancel?orderId=${encodeURIComponent(order.orderId)}`;
        });

        app.showToast(`Order ${order.orderNumber} created. Opening Razorpay checkout...`, "success");
        razorpay.open();
    }

    async function placeOrder() {
        const shippingAddress = shippingAddressInput.value.trim();
        const billingAddress = billingAddressInput.value.trim();
        const selectedMethod = document.querySelector("input[name=\"paymentMethodOption\"]:checked");
        const paymentMethod = selectedMethod ? selectedMethod.value : null;

        if (!shippingAddress || !billingAddress) {
            app.showToast("Shipping and billing addresses are required", "error");
            return;
        }
        if (!paymentMethod) {
            app.showToast("Please select a payment method", "error");
            return;
        }

        if (!cartState || !cartState.items || cartState.items.length === 0) {
            app.showToast("Cart is empty", "error");
            return;
        }

        try {
            const order = await app.api("/orders/checkout", {
                method: "POST",
                body: { shippingAddress, billingAddress, paymentMethod }
            });

            let paymentInfo = null;
            try {
                paymentInfo = await app.api("/payments/process", {
                    method: "POST",
                    body: {
                        orderId: order.orderId,
                        simulateFailure: false,
                        successUrl: `${window.location.origin}/buyer/payment-success`,
                        cancelUrl: `${window.location.origin}/buyer/payment-cancel`
                    }
                });
            } catch (paymentError) {
                app.showToast(paymentError.message || "Order placed, payment not processed", "error");
                return;
            }

            if (paymentInfo && paymentInfo.provider === "RAZORPAY" && paymentInfo.gatewayOrderId && paymentInfo.gatewayKeyId) {
                openRazorpayCheckout(order, paymentInfo);
                return;
            }

            const message = paymentInfo
                ? `Order ${order.orderNumber} placed. Payment ${paymentInfo.paymentStatus}.`
                : `Order ${order.orderNumber} placed successfully.`;

            app.showToast(message, "success");
            checkoutForm.reset();
            await loadCart();
            setTimeout(() => {
                window.location.href = "/buyer/orders";
            }, 550);
        } catch (error) {
            app.showToast(error.message || "Checkout failed", "error");
        }
    }

    cartTableBody.addEventListener("click", async (event) => {
        const target = event.target;
        if (target.classList.contains("update-qty-btn")) {
            const itemId = Number(target.dataset.itemId);
            const qtyInput = cartTableBody.querySelector(`.quantity-input[data-item-id="${itemId}"]`);
            const quantity = Number(qtyInput.value);
            if (!Number.isInteger(quantity) || quantity < 1) {
                app.showToast("Quantity must be at least 1", "error");
                return;
            }
            await updateItemQuantity(itemId, quantity);
        }
        if (target.classList.contains("remove-item-btn")) {
            await removeItem(Number(target.dataset.itemId));
        }
    });

    clearCartBtn.addEventListener("click", clearCart);
    paymentMethodOptions.forEach((option) => {
        option.addEventListener("change", () => {
            paymentMethodCards.forEach((card) => {
                const input = card.querySelector("input[name=\"paymentMethodOption\"]");
                card.classList.toggle("active", Boolean(input && input.checked));
            });
        });
    });
    checkoutForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await placeOrder();
    });

    loadCart();
});
