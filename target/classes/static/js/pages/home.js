"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    app.mountShell({
        active: "home",
        title: "RevShop - Premium Shopping Destination",
        subtitle: "Explore premium quality clothes, smart electronics, and trusted daily essentials from verified sellers."
    });

    const state = {
        page: 0,
        size: 9,
        hasNext: false,
        hasPrevious: false
    };

    const searchForm = document.getElementById("searchForm");
    const keywordInput = document.getElementById("keywordInput");
    const categorySelect = document.getElementById("categorySelect");
    const minPriceInput = document.getElementById("minPriceInput");
    const maxPriceInput = document.getElementById("maxPriceInput");
    const stockSelect = document.getElementById("stockSelect");
    const clearFiltersBtn = document.getElementById("clearFiltersBtn");

    const productGrid = document.getElementById("productGrid");
    const catalogMeta = document.getElementById("catalogMeta");
    const emptyCatalog = document.getElementById("emptyCatalog");

    const prevPageBtn = document.getElementById("prevPageBtn");
    const nextPageBtn = document.getElementById("nextPageBtn");
    const pageInfo = document.getElementById("pageInfo");

    function currentRole() {
        return app.getRole();
    }

    async function loadCategories() {
        try {
            const categories = await app.api("/categories");
            categories.forEach((category) => {
                const option = document.createElement("option");
                option.value = String(category.id);
                option.textContent = category.name;
                categorySelect.appendChild(option);
            });
        } catch (error) {
            app.showToast(error.message || "Failed to load categories", "error");
        }
    }

    function buildSearchQuery() {
        const params = new URLSearchParams();
        const keyword = keywordInput.value.trim();
        const categoryId = categorySelect.value;
        const minPrice = minPriceInput.value;
        const maxPrice = maxPriceInput.value;
        const inStock = stockSelect.value;

        if (keyword) params.set("keyword", keyword);
        if (categoryId) params.set("categoryId", categoryId);
        if (minPrice) params.set("minPrice", minPrice);
        if (maxPrice) params.set("maxPrice", maxPrice);
        if (inStock) params.set("inStock", inStock);
        params.set("page", String(state.page));
        params.set("size", String(state.size));
        params.set("sortBy", "createdAt");
        params.set("sortDir", "desc");
        return params.toString();
    }

    function buildStars(rating) {
        const value = Number.isFinite(Number(rating)) ? Number(rating) : 0;
        const safe = Math.max(0, Math.min(5, Math.round(value)));
        return `Rating ${safe}/5`;
    }

    function reviewerLabel(email) {
        if (!email) return "Verified Buyer";
        const atIndex = email.indexOf("@");
        const local = atIndex > 0 ? email.slice(0, atIndex) : email;
        const visible = local.slice(0, 2);
        return `${visible}${"*".repeat(Math.max(1, local.length - 2))}`;
    }

    function summarizeReviews(reviews) {
        const items = Array.isArray(reviews) ? reviews : [];
        if (items.length === 0) {
            return { totalReviews: 0, averageRating: null, latestReviews: [] };
        }
        const ratingSum = items.reduce((sum, review) => sum + Number(review.rating || 0), 0);
        const averageRating = ratingSum / items.length;
        return {
            totalReviews: items.length,
            averageRating,
            latestReviews: items.slice(0, 2)
        };
    }

    function renderReviewBlock(reviewData) {
        if (!reviewData || reviewData.totalReviews === 0) {
            return `
                <div class="product-review-block mt-3">
                    <div class="small fw-semibold mb-1">Customer Reviews</div>
                    <div class="small market-muted">No reviews yet.</div>
                </div>
            `;
        }

        const averageLabel = Number(reviewData.averageRating || 0).toFixed(1);
        const latest = (reviewData.latestReviews || []).map((review) => {
            const headline = review.title || "Verified purchase review";
            const detail = review.comment || "";
            return `
                <div class="product-review-item">
                    <div class="d-flex justify-content-between align-items-center gap-2">
                        <span class="reviewer-tag">${app.escapeHtml(reviewerLabel(review.buyerEmail))}</span>
                        <span class="review-stars">${app.escapeHtml(buildStars(review.rating))}</span>
                    </div>
                    <div class="small fw-semibold mt-1">${app.escapeHtml(headline)}</div>
                    ${detail ? `<div class="small market-muted">${app.escapeHtml(detail)}</div>` : ""}
                </div>
            `;
        }).join("");

        return `
            <div class="product-review-block mt-3">
                <div class="small fw-semibold mb-1">Customer Reviews</div>
                <div class="small market-muted mb-2">${app.escapeHtml(averageLabel)}/5 from ${reviewData.totalReviews} review(s)</div>
                ${latest}
            </div>
        `;
    }

    async function fetchReviewInsights(products) {
        const uniqueIds = [...new Set((products || []).map((p) => Number(p.id)).filter((id) => Number.isInteger(id) && id > 0))];
        const entries = await Promise.all(uniqueIds.map(async (productId) => {
            try {
                const reviews = await app.api(`/reviews/product/${productId}`);
                return [productId, summarizeReviews(reviews)];
            } catch (error) {
                return [productId, summarizeReviews([])];
            }
        }));
        return new Map(entries);
    }

    function renderProducts(products, reviewInsights = new Map()) {
        productGrid.innerHTML = "";
        if (!products || products.length === 0) {
            emptyCatalog.classList.remove("d-none");
            return;
        }
        emptyCatalog.classList.add("d-none");

        const isBuyer = currentRole() === "BUYER";

        products.forEach((product) => {
            const col = document.createElement("div");
            col.className = "col-md-6 col-xl-4";
            const reviewData = reviewInsights.get(Number(product.id)) || summarizeReviews([]);

            const imageUrl = product.imageUrls && product.imageUrls.length > 0
                ? product.imageUrls[0]
                : "https://placehold.co/600x420/F1F4FA/5B6A82?text=RevShop";
            const mrp = product.mrpPrice ?? product.price ?? 0;
            const discounted = product.discountedPrice ?? product.price ?? 0;
            const outOfStock = product.inStock === false || Number(product.stock) <= 0;

            col.innerHTML = `
                <article class="product-card">
                    <img class="product-thumb" src="${app.escapeHtml(imageUrl)}" alt="product image">
                    <div class="p-3">
                        <div class="d-flex justify-content-between align-items-start gap-2">
                            <h6 class="mb-1">${app.escapeHtml(product.name)}</h6>
                            ${outOfStock ? '<span class="pill danger">Out of stock</span>' : '<span class="pill success">In stock</span>'}
                        </div>
                        <p class="market-muted small mb-2">${app.escapeHtml(product.categoryName || "-")}</p>
                        <p class="small market-muted mb-2">${app.escapeHtml((product.description || "No description").slice(0, 85))}</p>
                        <div class="price-block mb-3">
                            <span class="price-current">${app.formatCurrency(discounted)}</span>
                            <span class="price-mrp">${app.formatCurrency(mrp)}</span>
                        </div>
                        <div class="d-grid gap-2">
                            ${
                                isBuyer
                                    ? `<button class="btn btn-brand market-btn add-cart-btn" data-product-id="${product.id}" ${outOfStock ? "disabled" : ""}>Add to Cart</button>
                                       <button class="btn btn-outline-primary market-btn add-wishlist-btn" data-product-id="${product.id}">Add to Wishlist</button>`
                                    : `<button class="btn btn-outline-secondary market-btn" disabled>Login as Buyer to purchase</button>`
                            }
                        </div>
                        ${renderReviewBlock(reviewData)}
                    </div>
                </article>
            `;
            productGrid.appendChild(col);
        });

        if (isBuyer) {
            productGrid.querySelectorAll(".add-cart-btn").forEach((btn) => {
                btn.addEventListener("click", () => addToCart(Number(btn.dataset.productId)));
            });
            productGrid.querySelectorAll(".add-wishlist-btn").forEach((btn) => {
                btn.addEventListener("click", () => addToWishlist(Number(btn.dataset.productId)));
            });
        }
    }

    function renderPagination(data) {
        state.hasNext = !!data.hasNext;
        state.hasPrevious = !!data.hasPrevious;
        const currentPage = (data.page ?? 0) + 1;
        const totalPages = Math.max(data.totalPages ?? 1, 1);
        pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
        prevPageBtn.disabled = !state.hasPrevious;
        nextPageBtn.disabled = !state.hasNext;
    }

    async function loadProducts() {
        try {
            const query = buildSearchQuery();
            const result = await app.api(`/products/search?${query}`);
            const products = result.content || [];
            const reviewInsights = await fetchReviewInsights(products);
            renderProducts(products, reviewInsights);
            renderPagination(result);
            catalogMeta.textContent = `${result.totalElements ?? 0} products found`;
        } catch (error) {
            productGrid.innerHTML = "";
            emptyCatalog.classList.remove("d-none");
            if (error.status === 401) {
                catalogMeta.textContent = "Login to browse products.";
                emptyCatalog.innerHTML = `
                    <h6 class="mb-2">Please login to continue</h6>
                    <p class="market-muted mb-3">Your APIs are secured with JWT.</p>
                    <a class="btn btn-brand market-btn" href="/login">Login Now</a>
                `;
            } else {
                catalogMeta.textContent = "Failed to load catalog";
                app.showToast(error.message || "Unable to fetch products", "error");
            }
        }
    }

    async function addToCart(productId) {
        try {
            await app.api("/cart/items", {
                method: "POST",
                body: { productId, quantity: 1 }
            });
            app.showToast("Added to cart", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to add to cart", "error");
        }
    }

    async function addToWishlist(productId) {
        try {
            await app.api("/wishlist/items", {
                method: "POST",
                body: { productId }
            });
            app.showToast("Added to wishlist", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to add to wishlist", "error");
        }
    }

    searchForm.addEventListener("submit", (event) => {
        event.preventDefault();
        state.page = 0;
        loadProducts();
    });

    clearFiltersBtn.addEventListener("click", () => {
        searchForm.reset();
        state.page = 0;
        loadProducts();
    });

    prevPageBtn.addEventListener("click", () => {
        if (!state.hasPrevious) return;
        state.page -= 1;
        loadProducts();
    });

    nextPageBtn.addEventListener("click", () => {
        if (!state.hasNext) return;
        state.page += 1;
        loadProducts();
    });

    Promise.all([loadCategories(), loadProducts()]).catch(() => {
        // handled in individual functions
    });
});
