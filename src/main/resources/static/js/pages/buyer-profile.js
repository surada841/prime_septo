"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;
    const DEFAULT_PROFILE_PHOTO = "/images/profile/avatar-placeholder.svg";
    const MAX_PROFILE_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    app.mountShell({
        active: "buyer-profile",
        title: "Buyer Profile",
        subtitle: "Keep your personal details updated for seamless checkout."
    });

    const emailText = document.getElementById("emailText");
    const roleText = document.getElementById("roleText");
    const statusText = document.getElementById("statusText");
    const profilePhotoPreview = document.getElementById("profilePhotoPreview");
    const photoUploadForm = document.getElementById("photoUploadForm");
    const photoFileInput = document.getElementById("photoFileInput");
    profilePhotoPreview.addEventListener("error", () => {
        profilePhotoPreview.src = DEFAULT_PROFILE_PHOTO;
    });

    const form = document.getElementById("buyerProfileForm");
    const firstNameInput = document.getElementById("firstNameInput");
    const lastNameInput = document.getElementById("lastNameInput");
    const phoneInput = document.getElementById("phoneInput");
    const addressInput = document.getElementById("addressInput");

    function resolveProfileImageUrl(value) {
        if (!value) return DEFAULT_PROFILE_PHOTO;
        const marker = "/uploads/";
        const idx = value.indexOf(marker);
        if (idx >= 0) return value.substring(idx);
        return value;
    }

    function renderProfile(profile) {
        emailText.textContent = profile.email || "-";
        roleText.textContent = profile.role || "-";
        statusText.textContent = profile.active ? "Active" : "Inactive";
        profilePhotoPreview.src = resolveProfileImageUrl(profile.profileImageUrl);

        firstNameInput.value = profile.firstName || "";
        lastNameInput.value = profile.lastName || "";
        phoneInput.value = profile.phone || "";
        addressInput.value = profile.address || "";
    }

    async function loadProfile() {
        try {
            const profile = await app.api("/profile/me");
            renderProfile(profile);
        } catch (error) {
            app.showToast(error.message || "Failed to load profile", "error");
        }
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            firstName: firstNameInput.value.trim(),
            lastName: lastNameInput.value.trim(),
            phone: phoneInput.value.trim() || null,
            address: addressInput.value.trim() || null
        };

        if (!payload.firstName || !payload.lastName) {
            app.showToast("First name and last name are required", "error");
            return;
        }

        try {
            const profile = await app.api("/profile/buyer", {
                method: "PUT",
                body: payload
            });
            renderProfile(profile);
            app.showToast("Profile updated successfully", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to update profile", "error");
        }
    });

    photoUploadForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const file = photoFileInput.files[0];
        if (!file) {
            app.showToast("Choose a profile photo first", "error");
            return;
        }
        if (file.type && !file.type.startsWith("image/")) {
            app.showToast("Only image files are allowed", "error");
            return;
        }
        if (file.size > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            app.showToast("Profile image is too large. Max allowed size is 5MB", "error");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);
        try {
            const profile = await app.api("/profile/photo", {
                method: "POST",
                body: formData
            });
            renderProfile(profile);
            photoUploadForm.reset();
            app.showToast("Profile photo updated", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to upload profile photo", "error");
        }
    });

    loadProfile();
});
