"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("BUYER")) return;

    app.mountShell({
        active: "wishlist",
        title: "Wishlist",
        subtitle: "Save products and move them to cart when ready."
    });

    const wishlistGrid = document.getElementById("wishlistGrid");
    const emptyWishlistState = document.getElementById("emptyWishlistState");

    function renderWishlist(items) {
        wishlistGrid.innerHTML = "";
        if (!items || items.length === 0) {
            emptyWishlistState.classList.remove("d-none");
            return;
        }
        emptyWishlistState.classList.add("d-none");

        items.forEach((item) => {
            const imageUrl = item.productImageUrl || "https://placehold.co/600x420/F1F4FA/5B6A82?text=RevShop";
            const col = document.createElement("div");
            col.className = "col-md-6 col-xl-4";
            col.innerHTML = `
                <article class="product-card">
                    <img class="product-thumb" src="${app.escapeHtml(imageUrl)}" alt="wishlist product">
                    <div class="p-3">
                        <h6 class="mb-1">${app.escapeHtml(item.productName)}</h6>
                        <div class="market-muted small mb-2">Seller: ${app.escapeHtml(item.sellerEmail || "-")}</div>
                        <div class="price-current mb-3">${app.formatCurrency(item.productPrice)}</div>
                        <div class="d-grid gap-2">
                            <button class="btn btn-brand market-btn move-cart-btn" data-product-id="${item.productId}">
                                Add to Cart
                            </button>
                            <button class="btn btn-outline-danger market-btn remove-wishlist-btn" data-item-id="${item.wishlistItemId}">
                                Remove
                            </button>
                        </div>
                    </div>
                </article>
            `;
            wishlistGrid.appendChild(col);
        });
    }

    async function loadWishlist() {
        try {
            const items = await app.api("/wishlist");
            renderWishlist(items);
        } catch (error) {
            app.showToast(error.message || "Failed to load wishlist", "error");
        }
    }

    async function moveToCart(productId) {
        try {
            await app.api("/cart/items", {
                method: "POST",
                body: { productId, quantity: 1 }
            });
            app.showToast("Item moved to cart", "success");
        } catch (error) {
            app.showToast(error.message || "Failed to add to cart", "error");
        }
    }

    async function removeWishlistItem(itemId) {
        try {
            await app.api(`/wishlist/items/${itemId}`, { method: "DELETE" });
            app.showToast("Wishlist item removed", "success");
            await loadWishlist();
        } catch (error) {
            app.showToast(error.message || "Failed to remove wishlist item", "error");
        }
    }

    wishlistGrid.addEventListener("click", async (event) => {
        const moveBtn = event.target.closest(".move-cart-btn");
        const removeBtn = event.target.closest(".remove-wishlist-btn");

        if (moveBtn) {
            await moveToCart(Number(moveBtn.dataset.productId));
        }
        if (removeBtn) {
            await removeWishlistItem(Number(removeBtn.dataset.itemId));
        }
    });

    loadWishlist();
});
