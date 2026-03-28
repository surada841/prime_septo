"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;

    const token = app.getToken();
    const role = app.getRole();
    if (token && role) {
        app.redirectAfterLogin(role);
        return;
    }

    const form = document.getElementById("loginForm");
    const emailInput = document.getElementById("emailInput");
    const passwordInput = document.getElementById("passwordInput");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = emailInput.value.trim();
        const password = passwordInput.value;
        if (!email || !password) {
            app.showToast("Email and password are required", "error");
            return;
        }

        try {
            const response = await app.api("/auth/login", {
                method: "POST",
                auth: false,
                body: { email, password }
            });

            app.setSession(response.token, response.role);
            app.showToast("Login successful", "success");
            setTimeout(() => app.redirectAfterLogin(response.role), 300);
        } catch (error) {
            app.showToast(error.message || "Login failed", "error");
        }
    });
});
