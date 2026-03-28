"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "notifications",
        title: "Notification Center",
        subtitle: "Stay updated on order, payment, and stock events."
    });

    const unreadOnlyToggle = document.getElementById("unreadOnlyToggle");
    const markAllReadBtn = document.getElementById("markAllReadBtn");
    const notificationList = document.getElementById("notificationList");
    const emptyNotificationState = document.getElementById("emptyNotificationState");

    function renderNotifications(items) {
        notificationList.innerHTML = "";
        if (!items || items.length === 0) {
            emptyNotificationState.classList.remove("d-none");
            return;
        }
        emptyNotificationState.classList.add("d-none");

        items.forEach((item) => {
            const wrapper = document.createElement("article");
            wrapper.className = "market-card flat p-3 mb-2";
            wrapper.innerHTML = `
                <div class="d-flex justify-content-between align-items-start gap-2">
                    <div>
                        <div class="fw-semibold">${app.escapeHtml(item.title)}</div>
                        <div class="small market-muted">${app.escapeHtml(item.message)}</div>
                        <div class="small market-muted mt-1">${app.formatDateTime(item.createdAt)}</div>
                    </div>
                    <div class="text-end">
                        <span class="pill ${item.isRead ? "success" : "warning"}">${item.isRead ? "Read" : "Unread"}</span>
                        ${
                            item.isRead
                                ? ""
                                : `<div class="mt-2"><button class="btn btn-sm btn-outline-primary mark-read-btn" data-id="${item.notificationId}">Mark as read</button></div>`
                        }
                    </div>
                </div>
            `;
            notificationList.appendChild(wrapper);
        });
    }

    async function loadNotifications() {
        const unreadOnly = unreadOnlyToggle.checked;
        try {
            const items = await app.api(`/notifications/my?unreadOnly=${unreadOnly}`);
            renderNotifications(items);
        } catch (error) {
            app.showToast(error.message || "Failed to load notifications", "error");
        }
    }

    async function markAsRead(notificationId) {
        try {
            await app.api(`/notifications/${notificationId}/read`, { method: "PATCH" });
            app.showToast("Notification marked as read", "success");
            await loadNotifications();
        } catch (error) {
            app.showToast(error.message || "Failed to update notification", "error");
        }
    }

    async function markAllRead() {
        try {
            await app.api("/notifications/my/read-all", { method: "PATCH" });
            app.showToast("All notifications marked as read", "success");
            await loadNotifications();
        } catch (error) {
            app.showToast(error.message || "Failed to mark all as read", "error");
        }
    }

    unreadOnlyToggle.addEventListener("change", loadNotifications);
    markAllReadBtn.addEventListener("click", markAllRead);

    notificationList.addEventListener("click", async (event) => {
        const button = event.target.closest(".mark-read-btn");
        if (!button) return;
        await markAsRead(Number(button.dataset.id));
    });

    loadNotifications();
});
