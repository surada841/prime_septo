"use strict";

window.RevShopApp = (() => {
    const API_BASE = "/api";
    const TOKEN_KEY = "revshop_token";
    const ROLE_KEY = "revshop_role";
    const THEME_KEY = "revshop_theme";
    const DEFAULT_PROFILE_PHOTO = "/images/profile/avatar-placeholder.svg";

    const currencyFormatter = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    });

    function safeStorageGet(key) {
        try {
            return localStorage.getItem(key);
        } catch (err) {
            return null;
        }
    }

    function safeStorageSet(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch (err) {
            // Ignore storage failures in private/restricted modes.
        }
    }

    function safeStorageRemove(key) {
        try {
            localStorage.removeItem(key);
        } catch (err) {
            // Ignore storage failures in private/restricted modes.
        }
    }

    function getToken() {
        return safeStorageGet(TOKEN_KEY);
    }

    function getRole() {
        return safeStorageGet(ROLE_KEY);
    }

    function setSession(token, role) {
        safeStorageSet(TOKEN_KEY, token);
        safeStorageSet(ROLE_KEY, String(role || "").toUpperCase());
    }

    function clearSession() {
        safeStorageRemove(TOKEN_KEY);
        safeStorageRemove(ROLE_KEY);
    }

    function getStoredTheme() {
        const theme = safeStorageGet(THEME_KEY);
        return theme === "dark" || theme === "light" ? theme : null;
    }

    function getPreferredTheme() {
        const stored = getStoredTheme();
        if (stored) {
            return stored;
        }
        if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
            return "dark";
        }
        return "light";
    }

    function applyTheme(theme) {
        const normalized = theme === "dark" ? "dark" : "light";
        const root = document.documentElement;
        if (root) {
            root.setAttribute("data-theme", normalized);
        }
        safeStorageSet(THEME_KEY, normalized);

        const themeToggleBtn = document.getElementById("themeToggleBtn");
        if (themeToggleBtn) {
            const nextMode = normalized === "dark" ? "Light" : "Dark";
            themeToggleBtn.textContent = `${nextMode} Mode`;
            themeToggleBtn.setAttribute("aria-label", `Switch to ${nextMode.toLowerCase()} mode`);
            themeToggleBtn.setAttribute("title", `Switch to ${nextMode.toLowerCase()} mode`);
        }
        return normalized;
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
        return applyTheme(current === "dark" ? "light" : "dark");
    }

    function decodeJwtPayload(token) {
        try {
            const parts = token.split(".");
            if (parts.length !== 3) return null;
            const payload = parts[1]
                .replace(/-/g, "+")
                .replace(/_/g, "/");
            const pad = payload.length % 4;
            const padded = payload + (pad ? "=".repeat(4 - pad) : "");
            const decoded = atob(padded);
            return JSON.parse(decoded);
        } catch (err) {
            return null;
        }
    }

    function getCurrentUserEmail() {
        const token = getToken();
        if (!token) return null;
        const payload = decodeJwtPayload(token);
        if (!payload) return null;
        return payload.sub || payload.email || null;
    }

    async function apiRaw(path, options = {}) {
        const method = options.method || "GET";
        const headers = Object.assign({}, options.headers || {});
        const body = options.body;
        const authRequired = options.auth !== false;

        if (authRequired) {
            const token = getToken();
            if (!token) {
                throw {
                    status: 401,
                    message: "Please login to continue",
                    errors: []
                };
            }
            headers.Authorization = `Bearer ${token}`;
        }

        const isFormData = body instanceof FormData;
        if (!isFormData && body != null) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(`${API_BASE}${path}`, {
            method,
            headers,
            body: body == null ? null : (isFormData ? body : JSON.stringify(body))
        });

        let payload = null;
        const text = await response.text();
        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (err) {
                payload = { success: response.ok, message: text };
            }
        } else {
            payload = { success: response.ok, message: response.statusText };
        }

        if (!response.ok || payload.success === false) {
            const message = payload.message || "Request failed";
            const error = {
                status: response.status,
                message,
                errors: payload.errors || []
            };
            throw error;
        }

        return payload;
    }

    async function api(path, options = {}) {
        const payload = await apiRaw(path, options);
        return payload.data;
    }

    function formatCurrency(value) {
        if (value == null || Number.isNaN(Number(value))) return currencyFormatter.format(0);
        return currencyFormatter.format(Number(value));
    }

    function formatDateTime(value) {
        if (!value) return "-";
        const date = new Date(value);
        return date.toLocaleString("en-IN", {
            year: "numeric",
            month: "short",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function escapeHtml(value) {
        if (value == null) return "";
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function getRoleLabel(role) {
        if (!role) return "Guest";
        return role === "SELLER" ? "Seller" : role === "BUYER" ? "Buyer" : role;
    }

    function getFallbackDisplayName(role, email) {
        if (email) {
            const atIndex = email.indexOf("@");
            const localPart = (atIndex > 0 ? email.slice(0, atIndex) : email).trim();
            if (localPart) {
                return localPart;
            }
        }
        return role ? `${getRoleLabel(role)} User` : "Guest";
    }

    function getDisplayNameFromProfile(profile, role, email) {
        if (profile) {
            const userName = String(profile.username || profile.userName || "").trim();
            if (userName) {
                return userName;
            }

            const fullName = [profile.firstName, profile.lastName]
                .filter(Boolean)
                .join(" ")
                .trim();
            if (fullName) {
                return fullName;
            }
        }
        return getFallbackDisplayName(role, email);
    }

    function resolveProfileImageUrl(value) {
        if (!value) return DEFAULT_PROFILE_PHOTO;
        const marker = "/uploads/";
        const markerIndex = value.indexOf(marker);
        if (markerIndex >= 0) {
            return value.substring(markerIndex);
        }
        return value;
    }

    function getRoleLinks(role) {
        if (role === "BUYER") {
            return [
                { key: "buyer-dashboard", label: "Buyer Dashboard", href: "/buyer/dashboard" },
                { key: "cart", label: "Cart", href: "/buyer/cart" },
                { key: "orders", label: "Orders", href: "/buyer/orders" },
                { key: "wishlist", label: "Wishlist", href: "/buyer/wishlist" },
                { key: "notifications", label: "Notifications", href: "/buyer/notifications" },
                { key: "buyer-profile", label: "Profile", href: "/buyer/profile" }
            ];
        }
        if (role === "SELLER") {
            return [
                { key: "seller-dashboard", label: "Seller Dashboard", href: "/seller/dashboard" },
                { key: "seller-orders", label: "Orders", href: "/seller/orders" },
                { key: "products", label: "Products", href: "/seller/products" },
                { key: "categories", label: "Categories", href: "/seller/categories" },
                { key: "seller-notifications", label: "Notifications", href: "/seller/notifications" },
                { key: "seller-profile", label: "Profile", href: "/seller/profile" }
            ];
        }
        return [];
    }

    async function hydrateShellProfile(role, email) {
        const nameNode = document.getElementById("shellUserName");
        const avatarNode = document.getElementById("shellUserAvatar");
        if (!nameNode || !avatarNode) return;

        avatarNode.onerror = () => {
            avatarNode.src = DEFAULT_PROFILE_PHOTO;
            avatarNode.onerror = null;
        };

        try {
            const profile = await api("/profile/me");
            nameNode.textContent = getDisplayNameFromProfile(profile, role, email);
            avatarNode.src = resolveProfileImageUrl(profile.profileImageUrl);
        } catch (err) {
            nameNode.textContent = getFallbackDisplayName(role, email);
            avatarNode.src = DEFAULT_PROFILE_PHOTO;
        }
    }

    function mountShell(options = {}) {
        const active = options.active || "home";
        const title = options.title || "RevShop";
        const subtitle = options.subtitle || "Premium fashion, electronics, and lifestyle marketplace.";
        const role = getRole();
        const email = getCurrentUserEmail();
        const fallbackDisplayName = getFallbackDisplayName(role, email);
        const host = document.getElementById("app-shell");
        if (!host) return;

        const roleLinks = getRoleLinks(role)
            .map(link => `
                <li class="nav-item">
                    <a class="nav-link ${active === link.key ? "active" : ""}" href="${link.href}">
                        ${link.label}
                    </a>
                </li>
            `).join("");

        const themeButton = `
            <button class="btn btn-sm btn-light market-btn theme-toggle-btn" id="themeToggleBtn" type="button">
                Dark Mode
            </button>
        `;

        const authButtons = role
            ? `
                <div class="d-flex align-items-center gap-2">
                    ${themeButton}
                    <span class="badge text-bg-light">${getRoleLabel(role)}</span>
                    <button class="btn btn-sm btn-light market-btn" id="logoutBtn">Logout</button>
                </div>
            `
            : `
                <div class="d-flex align-items-center gap-2">
                    ${themeButton}
                    <a class="btn btn-sm btn-light market-btn" href="/login">Login</a>
                    <a class="btn btn-sm btn-accent market-btn" href="/register">Register</a>
                </div>
            `;

        host.innerHTML = `
            <header class="shell-header">
                <div class="container py-2">
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
                        <a href="/" class="text-decoration-none d-flex align-items-center gap-2 text-white">
                            <span class="logo-chip font-display">RS</span>
                            <div>
                                <div class="font-display fs-5 fw-bold lh-1">RevShop</div>
                                <small class="opacity-75">Premium Marketplace</small>
                            </div>
                        </a>
                        ${authButtons}
                    </div>
                    <div class="d-flex align-items-center justify-content-between mt-2 flex-wrap gap-2">
                        <ul class="nav gap-1">
                            <li class="nav-item">
                                <a class="nav-link ${active === "home" ? "active" : ""}" href="/">Home</a>
                            </li>
                            ${roleLinks}
                        </ul>
                        ${role
                            ? `
                                <div class="shell-user-pill">
                                    <img
                                        id="shellUserAvatar"
                                        class="shell-user-avatar"
                                        src="${DEFAULT_PROFILE_PHOTO}"
                                        alt="Profile photo">
                                    <span class="shell-user-name" id="shellUserName">${escapeHtml(fallbackDisplayName)}</span>
                                </div>
                            `
                            : `<small class="opacity-75">Guest mode</small>`
                        }
                    </div>
                </div>
            </header>
            <section class="shell-hero">
                <div class="container py-4 py-md-5">
                    <h1 class="font-display fw-bold mb-1">${escapeHtml(title)}</h1>
                    <p class="hero-subtitle mb-0">${escapeHtml(subtitle)}</p>
                </div>
            </section>
        `;
        ensureSiteFooter();
        applyTheme(getPreferredTheme());

        const themeToggleBtn = document.getElementById("themeToggleBtn");
        if (themeToggleBtn) {
            themeToggleBtn.addEventListener("click", () => {
                toggleTheme();
            });
        }

        const logoutBtn = document.getElementById("logoutBtn");
        if (logoutBtn) {
            logoutBtn.addEventListener("click", () => {
                clearSession();
                showToast("Logged out successfully", "info");
                setTimeout(() => {
                    window.location.href = "/login";
                }, 350);
            });
        }

        if (role) {
            hydrateShellProfile(role, email);
        }
    }

    function ensureRole(allowedRoles) {
        const token = getToken();
        const role = getRole();
        if (!token || !role) {
            window.location.href = "/login";
            return false;
        }
        const allowed = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
        if (allowed.length > 0 && !allowed.includes(role)) {
            window.location.href = "/";
            return false;
        }
        return true;
    }

    function redirectAfterLogin(role) {
        if (role === "SELLER") {
            window.location.href = "/seller/dashboard";
            return;
        }
        if (role === "BUYER") {
            window.location.href = "/buyer/dashboard";
            return;
        }
        window.location.href = "/";
    }

    function ensureToastStack() {
        let stack = document.querySelector(".toast-stack");
        if (!stack) {
            stack = document.createElement("div");
            stack.className = "toast-stack";
            document.body.appendChild(stack);
        }
        return stack;
    }

    function ensureSiteFooter() {
        let footer = document.getElementById("revshopGlobalFooter");
        if (footer) {
            return footer;
        }

        const year = new Date().getFullYear();
        footer = document.createElement("footer");
        footer.id = "revshopGlobalFooter";
        footer.className = "revshop-site-footer";
        footer.innerHTML = `
            <div class="container">
                <div class="site-footer-grid">
                    <section>
                        <h6 class="font-display mb-1">RevShop</h6>
                        <p class="mb-0 market-muted">Premium quality clothes, electronics, and daily essentials in one trusted marketplace.</p>
                    </section>
                    <section>
                        <h6 class="font-display mb-1">Featured Categories</h6>
                        <p class="mb-0 market-muted">Luxury fashion, smart electronics, home upgrades, beauty care, and lifestyle picks.</p>
                    </section>
                    <section>
                        <h6 class="font-display mb-1">Why RevShop</h6>
                        <p class="mb-0 market-muted">Secure checkout, verified sellers, real-time order updates, and reliable support.</p>
                    </section>
                </div>
                <div class="site-footer-meta">
                    <span>Website developed by Rajukonde</span>
                    <span>Company location: Hyderabad, Telangana, India</span>
                    <span>&copy; ${year} RevShop</span>
                </div>
            </div>
        `;
        document.body.appendChild(footer);
        return footer;
    }

    function showToast(message, type = "info", timeout = 3400) {
        const stack = ensureToastStack();
        const toast = document.createElement("div");
        toast.className = `market-toast ${type}`;
        toast.textContent = message;
        stack.appendChild(toast);

        setTimeout(() => {
            toast.remove();
        }, timeout);
    }

    function readQueryParam(name) {
        const params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    applyTheme(getPreferredTheme());

    return {
        applyTheme,
        api,
        apiRaw,
        clearSession,
        decodeJwtPayload,
        ensureRole,
        escapeHtml,
        formatCurrency,
        formatDateTime,
        getCurrentUserEmail,
        getPreferredTheme,
        getRole,
        getToken,
        mountShell,
        readQueryParam,
        redirectAfterLogin,
        setSession,
        showToast,
        toggleTheme
    };
})();
