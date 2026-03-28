"use strict";

document.addEventListener("DOMContentLoaded", async () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "buyer-dashboard",
        title: "Buyer Command Center",
        subtitle: "Track cart, wishlist, orders, and account alerts in one place."
    });

    const cartCountKpi = document.getElementById("cartCountKpi");
    const wishlistCountKpi = document.getElementById("wishlistCountKpi");
    const ordersCountKpi = document.getElementById("ordersCountKpi");
    const unreadCountKpi = document.getElementById("unreadCountKpi");
    const recentNotifications = document.getElementById("recentNotifications");
    const trendingProducts = document.getElementById("trendingProducts");

    function renderNotifications(items) {
        if (!items || items.length === 0) {
            recentNotifications.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No recent notifications.</p></div>`;
            return;
        }
        recentNotifications.innerHTML = items.slice(0, 5).map((item) => `
            <div class="border rounded-3 p-2 mb-2 bg-light-subtle">
                <div class="fw-semibold">${app.escapeHtml(item.title)}</div>
                <div class="small market-muted">${app.escapeHtml(item.message)}</div>
                <div class="small market-muted">${app.formatDateTime(item.createdAt)}</div>
            </div>
        `).join("");
    }

    function renderTrendingProducts(items) {
        if (!items || items.length === 0) {
            trendingProducts.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No products available.</p></div>`;
            return;
        }
        trendingProducts.innerHTML = items.slice(0, 5).map((product) => `
            <div class="d-flex justify-content-between align-items-center border-bottom py-2">
                <div>
                    <div class="fw-semibold">${app.escapeHtml(product.name)}</div>
                    <small class="market-muted">${app.escapeHtml(product.categoryName || "-")}</small>
                </div>
                <div class="text-end">
                    <div class="fw-semibold">${app.formatCurrency(product.discountedPrice ?? product.price ?? 0)}</div>
                    <small class="market-muted">${product.inStock ? "In stock" : "Out of stock"}</small>
                </div>
            </div>
        `).join("");
    }

    try {
        const [cart, wishlist, orders, unread, notifications, products] = await Promise.all([
            app.api("/cart"),
            app.api("/wishlist"),
            app.api("/orders/my"),
            app.api("/notifications/my/unread-count"),
            app.api("/notifications/my?unreadOnly=true"),
            app.api("/products/search?page=0&size=6&sortBy=createdAt&sortDir=desc")
        ]);

        cartCountKpi.textContent = String(cart.totalItems || 0);
        wishlistCountKpi.textContent = String((wishlist || []).length);
        ordersCountKpi.textContent = String((orders || []).length);
        unreadCountKpi.textContent = String(unread.unreadCount || 0);
        renderNotifications(notifications || []);
        renderTrendingProducts(products.content || []);
    } catch (error) {
        app.showToast(error.message || "Failed to load buyer dashboard", "error");
    }
});
