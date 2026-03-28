"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;

    const tokenInput = document.getElementById("tokenInput");
    const newPasswordInput = document.getElementById("newPasswordInput");
    const form = document.getElementById("resetPasswordForm");

    const tokenFromQuery = app.readQueryParam("token");
    if (tokenFromQuery) {
        tokenInput.value = tokenFromQuery;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const token = tokenInput.value.trim();
        const newPassword = newPasswordInput.value;
        if (!token || !newPassword) {
            app.showToast("Token and new password are required", "error");
            return;
        }

        try {
            await app.api("/auth/password/reset", {
                method: "POST",
                auth: false,
                body: { token, newPassword }
            });
            app.showToast("Password reset successful", "success");
            setTimeout(() => {
                window.location.href = "/login";
            }, 450);
        } catch (error) {
            app.showToast(error.message || "Password reset failed", "error");
        }
    });
});
