"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("SELLER")) return;

    app.mountShell({
        active: "seller-orders",
        title: "Seller Orders",
        subtitle: "Advance lifecycle states from confirmed to final resolution."
    });

    const ordersList = document.getElementById("sellerOrdersList");
    const emptyState = document.getElementById("emptySellerOrdersState");

    function statusClass(status) {
        if (!status) return "warning";
        if (status === "DELIVERED" || status === "RETURNED" || status === "EXCHANGED") return "success";
        if (status === "CANCELLED") return "danger";
        return "warning";
    }

    function prettyStatus(status) {
        if (!status) return "-";
        return String(status).replace(/_/g, " ");
    }

    function paymentStatusClass(status) {
        if (!status || status === "PENDING" || status === "INITIATED") return "warning";
        if (status === "SUCCESS") return "success";
        if (status === "REFUNDED") return "warning";
        if (status === "FAILED") return "danger";
        return "warning";
    }

    function actionForStatus(orderId, status) {
        if (status === "CONFIRMED") {
            return `<button class="btn btn-outline-primary market-btn btn-sm seller-order-action-btn"
                            data-endpoint="/orders/seller/${orderId}/ship">
                        Mark Shipped
                    </button>`;
        }
        if (status === "SHIPPED") {
            return `<span class="small market-muted">Awaiting buyer delivery confirmation</span>`;
        }
        if (status === "RETURN_REQUESTED") {
            return `<button class="btn btn-outline-primary market-btn btn-sm seller-order-action-btn"
                            data-endpoint="/orders/seller/${orderId}/return/complete">
                        Complete Return
                    </button>`;
        }
        if (status === "EXCHANGE_REQUESTED") {
            return `<button class="btn btn-outline-primary market-btn btn-sm seller-order-action-btn"
                            data-endpoint="/orders/seller/${orderId}/exchange/complete">
                        Complete Exchange
                    </button>`;
        }
        return `<span class="small market-muted">No action available</span>`;
    }

    function renderOrders(orders) {
        ordersList.innerHTML = "";
        if (!orders || orders.length === 0) {
            emptyState.classList.remove("d-none");
            return;
        }

        emptyState.classList.add("d-none");
        orders.forEach((order) => {
            const card = document.createElement("article");
            card.className = "market-card flat p-3 mb-3";
            card.innerHTML = `
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <div>
                        <div class="fw-semibold">${app.escapeHtml(order.orderNumber)}</div>
                        <div class="market-muted small">Buyer: ${app.escapeHtml(order.buyerEmail || "-")}</div>
                        <div class="market-muted small">${app.formatDateTime(order.createdAt)}</div>
                    </div>
                    <div class="text-end">
                        <span class="pill ${statusClass(order.status)}">${app.escapeHtml(prettyStatus(order.status))}</span>
                        <div class="fw-semibold mt-1">${app.formatCurrency(order.totalAmount)}</div>
                    </div>
                </div>
                <div class="mt-2 small">
                    <div>
                        <strong>Payment:</strong>
                        ${app.escapeHtml(prettyStatus(order.paymentMethod || "-"))}
                        <span class="pill ${paymentStatusClass(order.paymentStatus)} ms-1">
                            ${app.escapeHtml(prettyStatus(order.paymentStatus || "-"))}
                        </span>
                    </div>
                    <div><strong>Shipping:</strong> ${app.escapeHtml(order.shippingAddress || "-")}</div>
                    <div><strong>Billing:</strong> ${app.escapeHtml(order.billingAddress || "-")}</div>
                    ${order.returnReason ? `<div><strong>Return Reason:</strong> ${app.escapeHtml(order.returnReason)}</div>` : ""}
                    ${order.exchangeReason ? `<div><strong>Exchange Reason:</strong> ${app.escapeHtml(order.exchangeReason)}</div>` : ""}
                    ${order.exchangeRequestedProductId ? `<div><strong>Exchange Product ID:</strong> ${app.escapeHtml(String(order.exchangeRequestedProductId))}</div>` : ""}
                </div>
                <div class="mt-3">
                    <div class="small fw-semibold mb-1">Items</div>
                    ${(order.items || []).map(item => `
                        <div class="d-flex justify-content-between border-top py-2 small">
                            <span>${app.escapeHtml(item.productName)} x ${item.quantity}</span>
                            <span>${app.formatCurrency(item.lineTotal)}</span>
                        </div>
                    `).join("")}
                </div>
                <div class="mt-3 d-flex justify-content-end">${actionForStatus(order.orderId, order.status)}</div>
            `;
            ordersList.appendChild(card);
        });
    }

    async function loadOrders() {
        try {
            const orders = await app.api("/orders/seller");
            renderOrders(orders);
        } catch (error) {
            app.showToast(error.message || "Failed to fetch seller orders", "error");
        }
    }

    ordersList.addEventListener("click", async (event) => {
        const button = event.target.closest(".seller-order-action-btn");
        if (!button) return;

        const endpoint = button.dataset.endpoint;
        if (!endpoint) return;

        button.disabled = true;
        try {
            await app.api(endpoint, { method: "PATCH", body: {} });
            app.showToast("Order status updated", "success");
            await loadOrders();
        } catch (error) {
            app.showToast(error.message || "Failed to update order", "error");
        } finally {
            button.disabled = false;
        }
    });

    loadOrders();
});
