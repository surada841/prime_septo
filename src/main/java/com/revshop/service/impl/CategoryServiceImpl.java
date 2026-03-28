package com.revshop.service.impl;

import com.revshop.dao.CategoryDAO;
import com.revshop.dao.ProductDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.category.CategoryCreateRequest;
import com.revshop.dto.category.CategoryResponse;
import com.revshop.dto.category.CategoryTreeResponse;
import com.revshop.dto.category.CategoryUpdateRequest;
import com.revshop.entity.Category;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ConflictException;
import com.revshop.exception.ForbiddenOperationException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    @Override
    @Transactional
    public CategoryResponse createCategory(String sellerEmail, CategoryCreateRequest request) {
        validateSeller(sellerEmail);
        String normalizedName = normalizeName(request.getName());
        String normalizedDescription = normalizeDescription(request.getDescription());
        String slug = buildSlug(normalizedName);

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryDAO.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        }

        Category existing = categoryDAO.findAnyByName(normalizedName).orElse(null);
        Category existingBySlug = categoryDAO.findBySlug(slug).orElse(null);
        if (existingBySlug != null && (existing == null || !existingBySlug.getId().equals(existing.getId()))) {
            throw new ConflictException("Category name already exists");
        }
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getActive()) && Boolean.FALSE.equals(existing.getIsDeleted())) {
                if (parent != null && parent.getId().equals(existing.getId())) {
                    throw new BadRequestException("Category cannot be its own parent");
                }
                // Idempotent create: update same-name category instead of failing with conflict.
                existing.setSlug(slug);
                existing.setDescription(normalizedDescription);
                existing.setParent(parent);
                return mapToResponse(categoryDAO.save(existing));
            }
            if (parent != null && parent.getId().equals(existing.getId())) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            existing.setName(normalizedName);
            existing.setSlug(slug);
            existing.setDescription(normalizedDescription);
            existing.setParent(parent);
            existing.setActive(true);
            existing.setIsDeleted(false);
            return mapToResponse(categoryDAO.save(existing));
        }

        Category category = Category.builder()
                .name(normalizedName)
                .slug(slug)
                .description(normalizedDescription)
                .parent(parent)
                .active(true)
                .build();

        return mapToResponse(categoryDAO.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, String sellerEmail, CategoryUpdateRequest request) {
        validateSeller(sellerEmail);
        String normalizedName = normalizeName(request.getName());
        String normalizedDescription = normalizeDescription(request.getDescription());
        String slug = buildSlug(normalizedName);

        Category category = categoryDAO.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Category duplicate = categoryDAO.findAnyByName(normalizedName).orElse(null);
        if (duplicate != null && !duplicate.getId().equals(category.getId())) {
            throw new ConflictException("Category name already exists");
        }
        Category duplicateBySlug = categoryDAO.findBySlug(slug).orElse(null);
        if (duplicateBySlug != null && !duplicateBySlug.getId().equals(category.getId())) {
            throw new ConflictException("Category name already exists");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            parent = categoryDAO.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            validateNoCycle(category, parent);
        }

        category.setName(normalizedName);
        category.setSlug(slug);
        category.setDescription(normalizedDescription);
        category.setParent(parent);

        return mapToResponse(categoryDAO.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, String sellerEmail) {
        validateSeller(sellerEmail);

        Category category = categoryDAO.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        long activeProductCount = productDAO.countActiveByCategoryId(categoryId);
        if (activeProductCount > 0) {
            throw new BadRequestException("Cannot delete category with active products");
        }

        long activeChildren = categoryDAO.countActiveChildren(categoryId);
        if (activeChildren > 0) {
            throw new BadRequestException("Cannot delete category with active child categories");
        }

        category.setActive(false);
        category.setIsDeleted(true);
        categoryDAO.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = categoryDAO.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryDAO.findAllActiveWithParent().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> categories = categoryDAO.findAllActiveWithParent();

        Map<Long, List<Category>> childrenByParentId = new HashMap<>();
        List<Category> roots = new ArrayList<>();
        Set<Long> activeIds = new HashSet<>();

        for (Category category : categories) {
            activeIds.add(category.getId());
        }

        for (Category category : categories) {
            Category parent = category.getParent();
            boolean parentVisible = parent != null
                    && activeIds.contains(parent.getId())
                    && Boolean.TRUE.equals(parent.getActive())
                    && Boolean.FALSE.equals(parent.getIsDeleted());

            if (!parentVisible) {
                roots.add(category);
            } else {
                childrenByParentId
                        .computeIfAbsent(parent.getId(), ignored -> new ArrayList<>())
                        .add(category);
            }
        }

        return roots.stream()
                .map(root -> mapToTree(root, childrenByParentId))
                .toList();
    }

    private CategoryTreeResponse mapToTree(Category category, Map<Long, List<Category>> childrenByParentId) {
        List<CategoryTreeResponse> children = childrenByParentId
                .getOrDefault(category.getId(), List.of())
                .stream()
                .map(child -> mapToTree(child, childrenByParentId))
                .toList();

        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .children(children)
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        Category parent = category.getParent();
        boolean parentVisible = parent != null
                && Boolean.TRUE.equals(parent.getActive())
                && Boolean.FALSE.equals(parent.getIsDeleted());
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(parentVisible ? parent.getId() : null)
                .parentName(parentVisible ? parent.getName() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private void validateNoCycle(Category category, Category newParent) {
        Category cursor = newParent;
        while (cursor != null) {
            if (cursor.getId().equals(category.getId())) {
                throw new BadRequestException("Category hierarchy cycle is not allowed");
            }
            cursor = cursor.getParent();
        }
    }

    private void validateSeller(String sellerEmail) {
        User seller = userDAO.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenOperationException("Only seller can manage categories");
        }
        if (!Boolean.TRUE.equals(seller.getActive()) || Boolean.TRUE.equals(seller.getIsDeleted())) {
            throw new ForbiddenOperationException("Seller account is inactive");
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new BadRequestException("Category name is required");
        }
        String normalized = name.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String buildSlug(String normalizedName) {
        String slug = normalizedName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isBlank()) {
            slug = "category-" + Math.abs(normalizedName.hashCode());
        }

        if (slug.length() > 120) {
            slug = slug.substring(0, 120).replaceAll("-+$", "");
            if (slug.isBlank()) {
                slug = "category-" + Math.abs(normalizedName.hashCode());
            }
        }
        return slug;
    }
}
