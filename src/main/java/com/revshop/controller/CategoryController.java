package com.revshop.controller;

import com.revshop.dto.category.CategoryCreateRequest;
import com.revshop.dto.category.CategoryResponse;
import com.revshop.dto.category.CategoryTreeResponse;
import com.revshop.dto.category.CategoryUpdateRequest;
import com.revshop.dto.common.ApiResponse;
import com.revshop.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category hierarchy and CRUD APIs")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            Authentication auth
    ) {
        CategoryResponse response = categoryService.createCategory(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Category created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request,
            Authentication auth
    ) {
        CategoryResponse response = categoryService.updateCategory(id, auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            Authentication auth
    ) {
        categoryService.deleteCategory(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category fetched", response));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> response = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success("Category tree fetched", response));
    }
}
