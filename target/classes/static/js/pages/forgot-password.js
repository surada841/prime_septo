"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;

    const form = document.getElementById("forgotPasswordForm");
    const emailInput = document.getElementById("emailInput");
    const tokenBox = document.getElementById("tokenBox");
    const tokenValue = document.getElementById("tokenValue");
    const copyTokenBtn = document.getElementById("copyTokenBtn");
    const goToResetLink = document.getElementById("goToResetLink");

    let latestToken = null;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const email = emailInput.value.trim();
        if (!email) {
            app.showToast("Email is required", "error");
            return;
        }

        try {
            const data = await app.api("/auth/password/forgot", {
                method: "POST",
                auth: false,
                body: { email }
            });

            latestToken = data.resetToken;
            tokenValue.textContent = data.resetToken;
            goToResetLink.href = `/reset-password?token=${encodeURIComponent(data.resetToken)}`;
            tokenBox.classList.remove("d-none");
            app.showToast("Reset token generated", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to generate token", "error");
        }
    });

    copyTokenBtn.addEventListener("click", async () => {
        if (!latestToken) {
            app.showToast("Generate token first", "error");
            return;
        }
        try {
            await navigator.clipboard.writeText(latestToken);
            app.showToast("Token copied", "success");
        } catch (error) {
            app.showToast("Copy failed. Please copy manually.", "error");
        }
    });
});
