package com.revshop.service;

import com.revshop.dto.category.CategoryCreateRequest;
import com.revshop.dto.category.CategoryResponse;
import com.revshop.dto.category.CategoryTreeResponse;
import com.revshop.dto.category.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(String sellerEmail, CategoryCreateRequest request);

    CategoryResponse updateCategory(Long categoryId, String sellerEmail, CategoryUpdateRequest request);

    void deleteCategory(Long categoryId, String sellerEmail);

    CategoryResponse getCategoryById(Long categoryId);

    List<CategoryResponse> getAllActiveCategories();

    List<CategoryTreeResponse> getCategoryTree();
}
