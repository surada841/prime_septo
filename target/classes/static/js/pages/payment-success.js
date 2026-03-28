"use strict";

document.addEventListener("DOMContentLoaded", async () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "orders",
        title: "Razorpay Payment Status",
        subtitle: "Reviewing your latest payment result."
    });

    const messageNode = document.getElementById("paymentSuccessMessage");
    const detailNode = document.getElementById("paymentSuccessDetails");
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get("orderId");

    if (!orderId) {
        messageNode.textContent = "Missing order details.";
        return;
    }

    try {
        const payment = await app.api(`/payments/order/${encodeURIComponent(orderId)}`);
        messageNode.textContent = `Payment ${payment.paymentStatus}. Order ${payment.orderNumber} is ${payment.orderStatus}.`;
        detailNode.innerHTML = `
            <div><strong>Amount:</strong> ${app.formatCurrency(payment.amount)}</div>
            <div><strong>Transaction Ref:</strong> ${app.escapeHtml(payment.transactionRef || "-")}</div>
            <div><strong>Gateway:</strong> ${app.escapeHtml(payment.gatewayResponse || "-")}</div>
            <div><strong>Provider:</strong> ${app.escapeHtml(payment.provider || "-")}</div>
        `;
    } catch (error) {
        messageNode.textContent = error.message || "Unable to verify payment.";
    }
});
