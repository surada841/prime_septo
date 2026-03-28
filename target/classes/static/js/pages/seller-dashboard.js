"use strict";

document.addEventListener("DOMContentLoaded", async () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("SELLER")) return;

    app.mountShell({
        active: "seller-dashboard",
        title: "Seller Control Tower",
        subtitle: "Revenue, orders, and inventory intelligence in one dashboard."
    });

    const overviewKpis = document.getElementById("overviewKpis");
    const recentOrdersTable = document.getElementById("recentOrdersTable");
    const topProductsTable = document.getElementById("topProductsTable");
    const recentAlerts = document.getElementById("recentAlerts");

    function prettyStatus(status) {
        if (!status) return "-";
        return String(status).replace(/_/g, " ");
    }

    function renderOverview(overview, unreadCount) {
        const tiles = [
            ["Total Products", overview.totalProducts],
            ["Active Products", overview.activeProducts],
            ["Low Stock", overview.lowStockProducts],
            ["Total Orders", overview.totalOrders],
            ["Pending Orders", overview.pendingOrders],
            ["Units Sold", overview.totalUnitsSold],
            ["Gross Revenue", app.formatCurrency(overview.grossRevenue || 0)],
            ["Unread Alerts", unreadCount || 0]
        ];

        overviewKpis.innerHTML = tiles.map(([label, value]) => `
            <div class="col-sm-6 col-lg-3">
                <div class="stat-tile">
                    <div class="market-muted small">${app.escapeHtml(label)}</div>
                    <div class="stat-kpi">${app.escapeHtml(String(value ?? 0))}</div>
                </div>
            </div>
        `).join("");
    }

    function renderRecentOrders(items) {
        if (!items || items.length === 0) {
            recentOrdersTable.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No recent seller orders.</p></div>`;
            return;
        }
        recentOrdersTable.innerHTML = `
            <div class="table-responsive">
                <table class="table market-table mb-0">
                    <thead>
                    <tr>
                        <th>Order</th>
                        <th>Buyer</th>
                        <th>Status</th>
                        <th class="text-end">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${items.map(item => `
                        <tr>
                            <td>
                                <div class="fw-semibold">${app.escapeHtml(item.orderNumber)}</div>
                                <small class="market-muted">${app.formatDateTime(item.orderedAt)}</small>
                            </td>
                            <td>${app.escapeHtml(item.buyerEmail)}</td>
                            <td>${app.escapeHtml(prettyStatus(item.orderStatus))}</td>
                            <td class="text-end">${app.formatCurrency(item.orderAmountForSeller)}</td>
                        </tr>
                    `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderTopProducts(items) {
        if (!items || items.length === 0) {
            topProductsTable.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No top products yet.</p></div>`;
            return;
        }
        topProductsTable.innerHTML = `
            <div class="table-responsive">
                <table class="table market-table mb-0">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th class="text-center">Units</th>
                        <th class="text-end">Revenue</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${items.map(item => `
                        <tr>
                            <td>
                                <div class="fw-semibold">${app.escapeHtml(item.productName)}</div>
                                <small class="market-muted">Stock ${item.currentStock}</small>
                            </td>
                            <td class="text-center">${item.unitsSold}</td>
                            <td class="text-end">${app.formatCurrency(item.revenue)}</td>
                        </tr>
                    `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderRecentAlerts(items) {
        if (!items || items.length === 0) {
            recentAlerts.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No alerts available.</p></div>`;
            return;
        }
        recentAlerts.innerHTML = items.slice(0, 5).map((item) => `
            <div class="border rounded-3 p-2 mb-2 bg-light-subtle">
                <div class="fw-semibold">${app.escapeHtml(item.title)}</div>
                <div class="small market-muted">${app.escapeHtml(item.message)}</div>
                <div class="small market-muted">${app.formatDateTime(item.createdAt)}</div>
            </div>
        `).join("");
    }

    try {
        const [data, unread, alerts] = await Promise.all([
            app.api("/seller/dashboard?recentLimit=8&topLimit=8&lowStockThreshold=5"),
            app.api("/notifications/my/unread-count"),
            app.api("/notifications/my?unreadOnly=true")
        ]);
        renderOverview(data.overview || {}, unread.unreadCount || 0);
        renderRecentOrders(data.recentOrders || []);
        renderTopProducts(data.topProducts || []);
        renderRecentAlerts(alerts || []);
    } catch (error) {
        app.showToast(error.message || "Failed to load seller dashboard", "error");
    }
});
