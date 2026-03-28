"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (app.getToken() && app.getRole()) {
        app.redirectAfterLogin(app.getRole());
        return;
    }

    const form = document.getElementById("registerForm");
    const roleSelect = document.getElementById("roleSelect");
    const buyerNameRow = document.getElementById("buyerNameRow");
    const sellerBusinessRow = document.getElementById("sellerBusinessRow");
    const addressRow = document.getElementById("addressRow");
    const gstRow = document.getElementById("gstRow");

    const firstNameInput = document.getElementById("firstNameInput");
    const lastNameInput = document.getElementById("lastNameInput");
    const businessNameInput = document.getElementById("businessNameInput");
    const gstInput = document.getElementById("gstInput");
    const emailInput = document.getElementById("emailInput");
    const phoneInput = document.getElementById("phoneInput");
    const passwordInput = document.getElementById("passwordInput");
    const confirmPasswordInput = document.getElementById("confirmPasswordInput");
    const addressInput = document.getElementById("addressInput");

    function renderRoleFields() {
        const role = roleSelect.value;
        const isSeller = role === "SELLER";
        buyerNameRow.classList.toggle("d-none", isSeller);
        sellerBusinessRow.classList.toggle("d-none", !isSeller);
        gstRow.classList.toggle("d-none", !isSeller);
        addressRow.querySelector(".form-label").textContent = isSeller ? "Business Address" : "Address";
    }

    roleSelect.addEventListener("change", renderRoleFields);
    renderRoleFields();

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const role = roleSelect.value;

        if (passwordInput.value !== confirmPasswordInput.value) {
            app.showToast("Passwords do not match", "error");
            return;
        }
        if (passwordInput.value.length < 6) {
            app.showToast("Password must be at least 6 characters", "error");
            return;
        }

        const common = {
            email: emailInput.value.trim(),
            password: passwordInput.value,
            phone: phoneInput.value.trim() || null
        };

        try {
            if (role === "BUYER") {
                if (!firstNameInput.value.trim() || !lastNameInput.value.trim()) {
                    app.showToast("First and last name are required for buyer", "error");
                    return;
                }
                await app.api("/auth/register/buyer", {
                    method: "POST",
                    auth: false,
                    body: {
                        ...common,
                        firstName: firstNameInput.value.trim(),
                        lastName: lastNameInput.value.trim(),
                        address: addressInput.value.trim() || null
                    }
                });
            } else {
                if (!businessNameInput.value.trim()) {
                    app.showToast("Business name is required for seller", "error");
                    return;
                }
                await app.api("/auth/register/seller", {
                    method: "POST",
                    auth: false,
                    body: {
                        ...common,
                        businessName: businessNameInput.value.trim(),
                        gstNumber: gstInput.value.trim() || null,
                        businessAddress: addressInput.value.trim() || null
                    }
                });
            }

            app.showToast("Registration successful. Please login.", "success");
            form.reset();
            renderRoleFields();
            setTimeout(() => {
                window.location.href = "/login";
            }, 500);
        } catch (error) {
            app.showToast(error.message || "Registration failed", "error");
        }
    });
});
