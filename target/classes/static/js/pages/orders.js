"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "orders",
        title: "Order History",
        subtitle: "Track order and payment lifecycle for every purchase."
    });

    const ordersList = document.getElementById("ordersList");
    const emptyOrdersState = document.getElementById("emptyOrdersState");
    const orderActionComposer = document.getElementById("orderActionComposer");
    const orderActionTitle = document.getElementById("orderActionTitle");
    const orderActionSubtitle = document.getElementById("orderActionSubtitle");
    const orderActionForm = document.getElementById("orderActionForm");
    const orderActionReasonInput = document.getElementById("orderActionReasonInput");
    const exchangeProductField = document.getElementById("exchangeProductField");
    const exchangeProductIdInput = document.getElementById("exchangeProductIdInput");
    const orderActionSubmitBtn = document.getElementById("orderActionSubmitBtn");
    const orderActionCloseBtn = document.getElementById("orderActionCloseBtn");
    const orderActionCancelBtn = document.getElementById("orderActionCancelBtn");
    const reviewComposer = document.getElementById("reviewComposer");
    const reviewComposerTitle = document.getElementById("reviewComposerTitle");
    const reviewComposerSubtitle = document.getElementById("reviewComposerSubtitle");
    const reviewComposerForm = document.getElementById("reviewComposerForm");
    const reviewRatingInput = document.getElementById("reviewRatingInput");
    const reviewTitleInput = document.getElementById("reviewTitleInput");
    const reviewCommentInput = document.getElementById("reviewCommentInput");
    const reviewComposerSubmitBtn = document.getElementById("reviewComposerSubmitBtn");
    const reviewComposerCloseBtn = document.getElementById("reviewComposerCloseBtn");
    const reviewComposerCancelBtn = document.getElementById("reviewComposerCancelBtn");

    let reviewsByProductId = new Map();
    let currentOrderAction = { orderId: null, action: null };
    let currentReviewContext = { productId: null, productName: "", existingReview: null };

    const ORDER_ACTION_META = {
        cancel: {
            title: "Cancel Order",
            subtitle: "Provide cancellation details for better support resolution.",
            submitLabel: "Submit Cancellation"
        },
        return: {
            title: "Request Return",
            subtitle: "Share reason for return and product condition details.",
            submitLabel: "Submit Return Request"
        },
        exchange: {
            title: "Request Exchange",
            subtitle: "Enter reason and optionally provide target product ID.",
            submitLabel: "Submit Exchange Request"
        }
    };

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

    function canReviewOrder(status) {
        return status === "DELIVERED" || status === "RETURNED" || status === "EXCHANGED";
    }

    function normalizeOptionalText(value) {
        if (value == null) return null;
        const trimmed = String(value).trim();
        return trimmed ? trimmed : null;
    }

    function buildReviewsIndex(reviews) {
        const map = new Map();
        (reviews || []).forEach((review) => {
            const productId = Number(review.productId);
            if (Number.isInteger(productId) && productId > 0) {
                map.set(productId, review);
            }
        });
        return map;
    }

    function buildReviewButton(orderStatus, item) {
        if (!canReviewOrder(orderStatus)) {
            return "";
        }
        const productId = Number(item.productId);
        const existing = reviewsByProductId.get(productId);
        const label = existing ? "Edit Review" : "Add Review";
        const productName = app.escapeHtml(item.productName || "Product");
        return `
            <button class="btn btn-outline-secondary market-btn btn-sm review-action-btn"
                    data-product-id="${productId}"
                    data-product-name="${productName}">
                ${label}
            </button>
        `;
    }

    function buildItemRows(order) {
        return (order.items || []).map((item) => `
            <div class="d-flex justify-content-between align-items-center border-top py-2 small gap-2">
                <span>${app.escapeHtml(item.productName)} x ${item.quantity}</span>
                <div class="d-flex align-items-center gap-2">
                    <span>${app.formatCurrency(item.lineTotal)}</span>
                    ${buildReviewButton(order.status, item)}
                </div>
            </div>
        `).join("");
    }

    function buildReasons(order) {
        const lines = [];
        if (order.cancelReason) lines.push(`<div class="small"><strong>Cancel Reason:</strong> ${app.escapeHtml(order.cancelReason)}</div>`);
        if (order.returnReason) lines.push(`<div class="small"><strong>Return Reason:</strong> ${app.escapeHtml(order.returnReason)}</div>`);
        if (order.exchangeReason) lines.push(`<div class="small"><strong>Exchange Reason:</strong> ${app.escapeHtml(order.exchangeReason)}</div>`);
        if (order.exchangeRequestedProductId) {
            lines.push(`<div class="small"><strong>Exchange Product ID:</strong> ${app.escapeHtml(String(order.exchangeRequestedProductId))}</div>`);
        }
        return lines.join("");
    }

    function buildActionButtons(order) {
        const buttons = [];
        if (order.canCancel) {
            buttons.push(`
                <button class="btn btn-outline-danger market-btn btn-sm order-action-btn"
                        data-order-id="${order.orderId}" data-action="cancel">
                    Cancel
                </button>
            `);
        }
        if (order.canReturn) {
            buttons.push(`
                <button class="btn btn-outline-primary market-btn btn-sm order-action-btn"
                        data-order-id="${order.orderId}" data-action="return">
                    Request Return
                </button>
            `);
        }
        if (order.canExchange) {
            buttons.push(`
                <button class="btn btn-outline-primary market-btn btn-sm order-action-btn"
                        data-order-id="${order.orderId}" data-action="exchange">
                    Request Exchange
                </button>
            `);
        }
        if (order.canConfirmDelivery) {
            buttons.push(`
                <button class="btn btn-outline-success market-btn btn-sm order-action-btn"
                        data-order-id="${order.orderId}" data-action="confirm-delivery">
                    Confirm Delivery
                </button>
            `);
        }
        buttons.push(`
            <button class="btn btn-outline-primary market-btn btn-sm payment-check-btn"
                    data-order-id="${order.orderId}">
                Check Payment
            </button>
        `);
        return buttons.join("");
    }

    function renderOrders(orders) {
        ordersList.innerHTML = "";
        if (!orders || orders.length === 0) {
            emptyOrdersState.classList.remove("d-none");
            return;
        }

        emptyOrdersState.classList.add("d-none");
        orders.forEach((order) => {
            const card = document.createElement("article");
            card.className = "market-card flat p-3 mb-3";
            card.innerHTML = `
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                    <div>
                        <div class="fw-semibold">${app.escapeHtml(order.orderNumber)}</div>
                        <div class="market-muted small">${app.formatDateTime(order.createdAt)}</div>
                    </div>
                    <div class="text-end">
                        <span class="pill ${statusClass(order.status)}">${app.escapeHtml(prettyStatus(order.status))}</span>
                        <div class="fw-semibold mt-1">${app.formatCurrency(order.totalAmount)}</div>
                    </div>
                </div>
                <div class="mt-2 small market-muted">
                    Payment: ${app.escapeHtml(prettyStatus(order.paymentMethod || "-"))}
                    | Status: ${app.escapeHtml(prettyStatus(order.paymentStatus || "-"))}
                </div>
                <div class="mt-2">
                    <div class="small"><strong>Shipping:</strong> ${app.escapeHtml(order.shippingAddress || "-")}</div>
                    <div class="small"><strong>Billing:</strong> ${app.escapeHtml(order.billingAddress || "-")}</div>
                </div>
                <div class="mt-2 market-muted">
                    ${buildReasons(order)}
                </div>
                <div class="mt-3">
                    <div class="small fw-semibold mb-1">Items</div>
                    ${buildItemRows(order)}
                </div>
                <div class="mt-3 d-flex justify-content-end flex-wrap gap-2">
                    ${buildActionButtons(order)}
                </div>
                <div class="small mt-2 market-muted payment-result" data-order-id="${order.orderId}"></div>
            `;
            ordersList.appendChild(card);
        });
    }

    async function loadOrders() {
        try {
            const orders = await app.api("/orders/my");
            try {
                const myReviews = await app.api("/reviews/my");
                reviewsByProductId = buildReviewsIndex(myReviews);
            } catch (reviewError) {
                reviewsByProductId = new Map();
            }
            renderOrders(orders);
        } catch (error) {
            app.showToast(error.message || "Failed to fetch orders", "error");
        }
    }

    function openOrderActionComposer(orderId, action) {
        const meta = ORDER_ACTION_META[action];
        if (!meta) return;

        currentOrderAction = { orderId, action };
        orderActionTitle.textContent = meta.title;
        orderActionSubtitle.textContent = meta.subtitle;
        orderActionSubmitBtn.textContent = meta.submitLabel;
        orderActionReasonInput.value = "";
        exchangeProductIdInput.value = "";
        exchangeProductField.classList.toggle("d-none", action !== "exchange");
        orderActionComposer.classList.remove("d-none");
        orderActionComposer.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function closeOrderActionComposer() {
        currentOrderAction = { orderId: null, action: null };
        orderActionReasonInput.value = "";
        exchangeProductIdInput.value = "";
        exchangeProductField.classList.add("d-none");
        orderActionComposer.classList.add("d-none");
    }

    function resolveOrderActionPath(orderId, action) {
        if (action === "cancel") return `/orders/my/${orderId}/cancel`;
        if (action === "return") return `/orders/my/${orderId}/return`;
        if (action === "exchange") return `/orders/my/${orderId}/exchange`;
        if (action === "confirm-delivery") return `/orders/my/${orderId}/confirm-delivery`;
        return null;
    }

    async function submitQuickOrderAction(orderId, action) {
        const path = resolveOrderActionPath(orderId, action);
        if (!path) return false;
        await app.api(path, { method: "PATCH", body: {} });
        return true;
    }

    async function submitComposedOrderAction() {
        const { orderId, action } = currentOrderAction;
        if (!orderId || !action) return false;

        const path = resolveOrderActionPath(orderId, action);
        if (!path) return false;

        const body = {};
        const reason = normalizeOptionalText(orderActionReasonInput.value);
        if (reason) {
            body.reason = reason;
        }

        if (action === "exchange") {
            const exchangeValue = normalizeOptionalText(exchangeProductIdInput.value);
            if (exchangeValue) {
                const exchangeProductId = Number(exchangeValue);
                if (!Number.isInteger(exchangeProductId) || exchangeProductId < 1) {
                    app.showToast("Exchange product ID must be a positive whole number", "error");
                    return false;
                }
                body.exchangeProductId = exchangeProductId;
            }
        }

        await app.api(path, { method: "PATCH", body });
        return true;
    }

    function openReviewComposer(productId, productName) {
        const existingReview = reviewsByProductId.get(productId) || null;
        currentReviewContext = { productId, productName: productName || "Product", existingReview };

        reviewComposerTitle.textContent = existingReview ? "Edit Product Review" : "Add Product Review";
        reviewComposerSubtitle.textContent = currentReviewContext.productName;
        reviewComposerSubmitBtn.textContent = existingReview ? "Update Review" : "Save Review";

        reviewRatingInput.value = String(existingReview?.rating || 5);
        reviewTitleInput.value = existingReview?.title || "";
        reviewCommentInput.value = existingReview?.comment || "";

        reviewComposer.classList.remove("d-none");
        reviewComposer.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function closeReviewComposer() {
        currentReviewContext = { productId: null, productName: "", existingReview: null };
        reviewRatingInput.value = "5";
        reviewTitleInput.value = "";
        reviewCommentInput.value = "";
        reviewComposer.classList.add("d-none");
    }

    async function submitReviewFromComposer() {
        const { productId, existingReview } = currentReviewContext;
        if (!productId) return false;

        const rating = Number(reviewRatingInput.value);
        if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
            app.showToast("Rating must be a whole number between 1 and 5", "error");
            return false;
        }

        const body = {
            rating,
            title: normalizeOptionalText(reviewTitleInput.value),
            comment: normalizeOptionalText(reviewCommentInput.value)
        };

        if (existingReview) {
            await app.api(`/reviews/${existingReview.reviewId}`, { method: "PUT", body });
            app.showToast("Review updated", "success");
            return true;
        }

        await app.api("/reviews", {
            method: "POST",
            body: { productId, ...body }
        });
        app.showToast("Review added", "success");
        return true;
    }

    ordersList.addEventListener("click", async (event) => {
        const reviewButton = event.target.closest(".review-action-btn");
        if (reviewButton) {
            const productId = Number(reviewButton.dataset.productId);
            if (!Number.isInteger(productId) || productId < 1) return;
            const productName = reviewButton.dataset.productName || "Product";

            openReviewComposer(productId, productName);
            return;
        }

        const actionButton = event.target.closest(".order-action-btn");
        if (actionButton) {
            const orderId = Number(actionButton.dataset.orderId);
            const action = actionButton.dataset.action;
            if (!Number.isInteger(orderId) || orderId < 1) return;

            try {
                if (action === "confirm-delivery") {
                    actionButton.disabled = true;
                    const changed = await submitQuickOrderAction(orderId, action);
                    if (changed) {
                        app.showToast("Delivery confirmed", "success");
                        await loadOrders();
                    }
                } else {
                    openOrderActionComposer(orderId, action);
                }
            } catch (error) {
                app.showToast(error.message || "Failed to update order", "error");
            } finally {
                if (action === "confirm-delivery") {
                    actionButton.disabled = false;
                }
            }
            return;
        }

        const button = event.target.closest(".payment-check-btn");
        if (!button) return;

        const orderId = Number(button.dataset.orderId);
        const resultNode = ordersList.querySelector(`.payment-result[data-order-id="${orderId}"]`);
        try {
            const payment = await app.api(`/payments/order/${orderId}`);
            if (resultNode) {
                resultNode.textContent = `Payment Status: ${payment.paymentStatus} | Order Status: ${payment.orderStatus}`;
            }
            app.showToast("Payment fetched", "success");
        } catch (error) {
            if (resultNode) {
                resultNode.textContent = error.message || "Payment not available";
            }
            app.showToast(error.message || "Payment fetch failed", "error");
        }
    });

    orderActionForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const changed = await submitComposedOrderAction();
            if (changed) {
                app.showToast("Order updated", "success");
                closeOrderActionComposer();
                await loadOrders();
            }
        } catch (error) {
            app.showToast(error.message || "Failed to update order", "error");
        }
    });

    orderActionCloseBtn?.addEventListener("click", closeOrderActionComposer);
    orderActionCancelBtn?.addEventListener("click", closeOrderActionComposer);

    reviewComposerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const changed = await submitReviewFromComposer();
            if (changed) {
                closeReviewComposer();
                await loadOrders();
            }
        } catch (error) {
            app.showToast(error.message || "Failed to save review", "error");
        }
    });

    reviewComposerCloseBtn?.addEventListener("click", closeReviewComposer);
    reviewComposerCancelBtn?.addEventListener("click", closeReviewComposer);

    loadOrders();
});
