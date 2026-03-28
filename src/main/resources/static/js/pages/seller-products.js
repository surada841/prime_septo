"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("SELLER")) return;
    const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

    app.mountShell({
        active: "products",
        title: "Product and Inventory Manager",
        subtitle: "Create catalog entries, adjust stock rules, and maintain product media."
    });

    const productForm = document.getElementById("productForm");
    const productIdInput = document.getElementById("productIdInput");
    const nameInput = document.getElementById("nameInput");
    const descriptionInput = document.getElementById("descriptionInput");
    const mrpInput = document.getElementById("mrpInput");
    const discountedPriceInput = document.getElementById("discountedPriceInput");
    const stockInput = document.getElementById("stockInput");
    const thresholdInput = document.getElementById("thresholdInput");
    const categoryInput = document.getElementById("categoryInput");
    const saveProductBtn = document.getElementById("saveProductBtn");
    const cancelEditBtn = document.getElementById("cancelEditBtn");

    const imageUploadForm = document.getElementById("imageUploadForm");
    const imageProductSelect = document.getElementById("imageProductSelect");
    const imageFileInput = document.getElementById("imageFileInput");

    const refreshProductsBtn = document.getElementById("refreshProductsBtn");
    const sellerProductsTableBody = document.getElementById("sellerProductsTableBody");
    const emptyProductsState = document.getElementById("emptyProductsState");
    const lowStockContainer = document.getElementById("lowStockContainer");

    let categories = [];
    let products = [];

    function toNumber(value) {
        const num = Number(value);
        return Number.isFinite(num) ? num : null;
    }

    function resetProductForm() {
        productIdInput.value = "";
        productForm.reset();
        saveProductBtn.textContent = "Save Product";
        cancelEditBtn.classList.add("d-none");
    }

    function fillCategoryOptions() {
        const options = ['<option value="">Select category</option>'].concat(
            categories.map((c) => `<option value="${c.id}">${app.escapeHtml(c.name)}</option>`)
        );
        categoryInput.innerHTML = options.join("");
    }

    function fillImageProductOptions() {
        const options = ['<option value="">Select product</option>'].concat(
            products.map((p) => `<option value="${p.id}">${app.escapeHtml(p.name)}</option>`)
        );
        imageProductSelect.innerHTML = options.join("");
    }

    async function loadCategories() {
        categories = await app.api("/categories");
        fillCategoryOptions();
    }

    function renderProductsTable() {
        sellerProductsTableBody.innerHTML = "";
        if (!products || products.length === 0) {
            emptyProductsState.classList.remove("d-none");
            return;
        }
        emptyProductsState.classList.add("d-none");

        products.forEach((product) => {
            const threshold = product.lowStockThreshold ?? 5;
            const isLowStock = Number(product.stock || 0) <= threshold;
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>
                    <div class="fw-semibold">${app.escapeHtml(product.name)}</div>
                    <small class="market-muted">${app.escapeHtml(product.categoryName || "-")}</small>
                </td>
                <td class="text-end">
                    <div>${app.formatCurrency(product.discountedPrice ?? product.price)}</div>
                    <small class="market-muted text-decoration-line-through">${app.formatCurrency(product.mrpPrice ?? product.price)}</small>
                </td>
                <td class="text-center">
                    <span class="pill ${isLowStock ? "danger" : "success"}">${product.stock}</span>
                </td>
                <td class="text-center">${threshold}</td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary edit-product-btn" data-id="${product.id}">Edit</button>
                        <button class="btn btn-outline-secondary threshold-product-btn" data-id="${product.id}">Threshold</button>
                        <button class="btn btn-outline-danger delete-product-btn" data-id="${product.id}">Delete</button>
                    </div>
                </td>
            `;
            sellerProductsTableBody.appendChild(row);
        });
    }

    function renderLowStock(items) {
        if (!items || items.length === 0) {
            lowStockContainer.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">No low-stock products currently.</p></div>`;
            return;
        }

        lowStockContainer.innerHTML = `
            <div class="table-responsive">
                <table class="table market-table mb-0">
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th class="text-center">Stock</th>
                        <th class="text-center">Threshold</th>
                        <th class="text-end">Price</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${items.map(item => `
                        <tr>
                            <td>${app.escapeHtml(item.name)}</td>
                            <td class="text-center">${item.stock}</td>
                            <td class="text-center">${item.lowStockThreshold ?? 5}</td>
                            <td class="text-end">${app.formatCurrency(item.discountedPrice ?? item.price)}</td>
                        </tr>
                    `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    async function loadProducts() {
        products = await app.api("/products/my");
        renderProductsTable();
        fillImageProductOptions();
    }

    async function loadLowStock() {
        const lowStockProducts = await app.api("/products/my/low-stock");
        renderLowStock(lowStockProducts);
    }

    function buildPayload() {
        const mrp = toNumber(mrpInput.value);
        const discounted = toNumber(discountedPriceInput.value);
        const stock = toNumber(stockInput.value);
        const categoryId = toNumber(categoryInput.value);
        const threshold = thresholdInput.value === "" ? null : toNumber(thresholdInput.value);

        if (!nameInput.value.trim() || !categoryId || !mrp || !discounted || stock == null) {
            throw new Error("Name, category, price, and stock are required");
        }
        if (discounted > mrp) {
            throw new Error("Discounted price cannot be greater than MRP");
        }
        return {
            name: nameInput.value.trim(),
            description: descriptionInput.value.trim() || null,
            price: discounted,
            mrpPrice: mrp,
            discountedPrice: discounted,
            stock,
            categoryId,
            lowStockThreshold: threshold
        };
    }

    function enterEditMode(productId) {
        const product = products.find((p) => Number(p.id) === Number(productId));
        if (!product) return;

        productIdInput.value = String(product.id);
        nameInput.value = product.name || "";
        descriptionInput.value = product.description || "";
        mrpInput.value = product.mrpPrice ?? product.price ?? "";
        discountedPriceInput.value = product.discountedPrice ?? product.price ?? "";
        stockInput.value = product.stock ?? "";
        thresholdInput.value = product.lowStockThreshold ?? 5;
        categoryInput.value = String(product.categoryId || "");
        saveProductBtn.textContent = "Update Product";
        cancelEditBtn.classList.remove("d-none");
    }

    async function saveProduct() {
        let payload;
        try {
            payload = buildPayload();
        } catch (error) {
            app.showToast(error.message, "error");
            return;
        }

        const productId = productIdInput.value;
        try {
            if (productId) {
                await app.api(`/products/${productId}`, {
                    method: "PUT",
                    body: payload
                });
                app.showToast("Product updated", "success");
            } else {
                await app.api("/products", {
                    method: "POST",
                    body: payload
                });
                app.showToast("Product created", "success");
            }
            resetProductForm();
            await Promise.all([loadProducts(), loadLowStock()]);
        } catch (error) {
            app.showToast(error.message || "Failed to save product", "error");
        }
    }

    async function deleteProduct(productId) {
        try {
            await app.api(`/products/${productId}`, { method: "DELETE" });
            app.showToast("Product deleted", "success");
            await Promise.all([loadProducts(), loadLowStock()]);
        } catch (error) {
            app.showToast(error.message || "Failed to delete product", "error");
        }
    }

    async function updateThreshold(productId) {
        const input = prompt("Enter new low-stock threshold (>= 0):");
        if (input == null) return;
        const threshold = toNumber(input);
        if (threshold == null || threshold < 0) {
            app.showToast("Invalid threshold value", "error");
            return;
        }

        try {
            await app.api(`/products/${productId}/low-stock-threshold`, {
                method: "PATCH",
                body: { lowStockThreshold: threshold }
            });
            app.showToast("Low-stock threshold updated", "success");
            await Promise.all([loadProducts(), loadLowStock()]);
        } catch (error) {
            app.showToast(error.message || "Failed to update threshold", "error");
        }
    }

    async function uploadImage() {
        const productId = toNumber(imageProductSelect.value);
        const file = imageFileInput.files[0];
        if (!productId || !file) {
            app.showToast("Select a product and image file", "error");
            return;
        }
        if (file.type && !file.type.startsWith("image/")) {
            app.showToast("Only image files are allowed", "error");
            return;
        }
        if (file.size > MAX_IMAGE_SIZE_BYTES) {
            app.showToast("Image is too large. Max allowed size is 10MB", "error");
            return;
        }

        const formData = new FormData();
        formData.append("files", file);
        try {
            await app.api(`/products/${productId}/images`, {
                method: "POST",
                body: formData
            });
            app.showToast("Image uploaded", "success");
            imageUploadForm.reset();
            await loadProducts();
        } catch (error) {
            app.showToast(error.message || "Image upload failed", "error");
        }
    }

    productForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await saveProduct();
    });

    cancelEditBtn.addEventListener("click", () => {
        resetProductForm();
    });

    imageUploadForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await uploadImage();
    });

    sellerProductsTableBody.addEventListener("click", async (event) => {
        const editBtn = event.target.closest(".edit-product-btn");
        const thresholdBtn = event.target.closest(".threshold-product-btn");
        const deleteBtn = event.target.closest(".delete-product-btn");

        if (editBtn) {
            enterEditMode(Number(editBtn.dataset.id));
        }
        if (thresholdBtn) {
            await updateThreshold(Number(thresholdBtn.dataset.id));
        }
        if (deleteBtn) {
            deleteBtn.disabled = true;
            await deleteProduct(Number(deleteBtn.dataset.id));
        }
    });

    refreshProductsBtn.addEventListener("click", async () => {
        try {
            await Promise.all([loadProducts(), loadLowStock()]);
            app.showToast("Product list refreshed", "info");
        } catch (error) {
            app.showToast(error.message || "Refresh failed", "error");
        }
    });

    Promise.all([loadCategories(), loadProducts(), loadLowStock()])
        .catch((error) => {
            app.showToast(error.message || "Failed to initialize seller products page", "error");
        });
});
