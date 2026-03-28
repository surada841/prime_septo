"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const app = window.RevShopApp;
    if (!app.ensureRole("SELLER")) return;

    app.mountShell({
        active: "categories",
        title: "Category Management",
        subtitle: "Design hierarchical catalog taxonomy for your marketplace."
    });

    const categoryForm = document.getElementById("categoryForm");
    const categoryIdInput = document.getElementById("categoryIdInput");
    const categoryNameInput = document.getElementById("categoryNameInput");
    const categoryDescriptionInput = document.getElementById("categoryDescriptionInput");
    const parentCategoryInput = document.getElementById("parentCategoryInput");
    const cancelCategoryEditBtn = document.getElementById("cancelCategoryEditBtn");

    const categoriesTableBody = document.getElementById("categoriesTableBody");
    const emptyCategoriesState = document.getElementById("emptyCategoriesState");
    const categoryTreeContainer = document.getElementById("categoryTreeContainer");

    let categories = [];

    function resetForm() {
        categoryIdInput.value = "";
        categoryForm.reset();
        parentCategoryInput.value = "";
        cancelCategoryEditBtn.classList.add("d-none");
    }

    function fillParentOptions() {
        const options = ['<option value="">No parent (root category)</option>'].concat(
            categories.map((c) => `<option value="${c.id}">${app.escapeHtml(c.name)}</option>`)
        );
        parentCategoryInput.innerHTML = options.join("");
    }

    function renderCategoriesTable() {
        categoriesTableBody.innerHTML = "";
        if (!categories || categories.length === 0) {
            emptyCategoriesState.classList.remove("d-none");
            return;
        }
        emptyCategoriesState.classList.add("d-none");

        categories.forEach((category) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>
                    <div class="fw-semibold">${app.escapeHtml(category.name)}</div>
                    <small class="market-muted">${app.escapeHtml(category.description || "")}</small>
                </td>
                <td>${app.escapeHtml(category.parentName || "Root")}</td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary edit-category-btn" data-id="${category.id}">Edit</button>
                        <button class="btn btn-outline-danger delete-category-btn" data-id="${category.id}">Delete</button>
                    </div>
                </td>
            `;
            categoriesTableBody.appendChild(row);
        });
    }

    function renderTreeNodes(nodes, level = 0) {
        if (!nodes || nodes.length === 0) return "";
        return `
            <ul class="list-unstyled ms-${Math.min(level * 2, 5)}">
                ${nodes.map(node => `
                    <li class="mb-2">
                        <div class="border rounded p-2 bg-light-subtle">
                            <div class="fw-semibold">${app.escapeHtml(node.name)}</div>
                            <div class="small market-muted">${app.escapeHtml(node.description || "")}</div>
                        </div>
                        ${renderTreeNodes(node.children || [], level + 1)}
                    </li>
                `).join("")}
            </ul>
        `;
    }

    async function loadCategoryTree() {
        const tree = await app.api("/categories/tree");
        if (!tree || tree.length === 0) {
            categoryTreeContainer.innerHTML = `<div class="empty-state"><p class="mb-0 market-muted">Category tree is empty.</p></div>`;
            return;
        }
        categoryTreeContainer.innerHTML = renderTreeNodes(tree);
    }

    async function loadCategories() {
        categories = await app.api("/categories");
        renderCategoriesTable();
        fillParentOptions();
    }

    function editCategory(id) {
        const category = categories.find((c) => Number(c.id) === Number(id));
        if (!category) return;
        categoryIdInput.value = String(category.id);
        categoryNameInput.value = category.name || "";
        categoryDescriptionInput.value = category.description || "";
        parentCategoryInput.value = category.parentId ? String(category.parentId) : "";
        cancelCategoryEditBtn.classList.remove("d-none");
    }

    async function removeCategory(id) {
        try {
            await app.api(`/categories/${id}`, { method: "DELETE" });
            app.showToast("Category deleted", "success");
            await Promise.all([loadCategories(), loadCategoryTree()]);
        } catch (error) {
            app.showToast(error.message || "Failed to delete category", "error");
        }
    }

    categoryForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const name = categoryNameInput.value.trim();
        if (!name) {
            app.showToast("Category name is required", "error");
            return;
        }

        const payload = {
            name,
            description: categoryDescriptionInput.value.trim() || null,
            parentId: parentCategoryInput.value ? Number(parentCategoryInput.value) : null
        };

        const categoryId = categoryIdInput.value;
        try {
            if (categoryId) {
                await app.api(`/categories/${categoryId}`, { method: "PUT", body: payload });
                app.showToast("Category updated", "success");
            } else {
                await app.api("/categories", { method: "POST", body: payload });
                app.showToast("Category created", "success");
            }
            resetForm();
            await Promise.all([loadCategories(), loadCategoryTree()]);
        } catch (error) {
            app.showToast(error.message || "Failed to save category", "error");
        }
    });

    cancelCategoryEditBtn.addEventListener("click", () => {
        resetForm();
    });

    categoriesTableBody.addEventListener("click", async (event) => {
        const editBtn = event.target.closest(".edit-category-btn");
        const deleteBtn = event.target.closest(".delete-category-btn");

        if (editBtn) {
            editCategory(Number(editBtn.dataset.id));
        }
        if (deleteBtn) {
            const ok = confirm("Delete this category?");
            if (ok) {
                await removeCategory(Number(deleteBtn.dataset.id));
            }
        }
    });

    Promise.all([loadCategories(), loadCategoryTree()])
        .catch((error) => {
            app.showToast(error.message || "Failed to initialize categories page", "error");
        });
});
