package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.common.PagedResponse;
import com.revshop.dto.product.ProductCreateRequest;
import com.revshop.dto.product.ProductImageResponse;
import com.revshop.dto.product.ProductResponse;
import com.revshop.dto.product.ProductUpdateRequest;
import com.revshop.dto.product.UpdateLowStockThresholdRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management and product catalog APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            Authentication auth
    ) {
        ProductResponse response = productService.createProduct(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            Authentication auth
    ) {
        ProductResponse response = productService.updateProduct(id, auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id, Authentication auth) {
        productService.deleteProduct(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> myProducts(Authentication auth) {
        List<ProductResponse> response = productService.getSellerProducts(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Seller products fetched", response));
    }

    @GetMapping("/my/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> myLowStockProducts(Authentication auth) {
        List<ProductResponse> response = productService.getMyLowStockProducts(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Low stock products fetched", response));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> publicProducts() {
        List<ProductResponse> response = productService.getAllActiveProducts();
        return ResponseEntity.ok(ApiResponse.success("Active products fetched", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched", response));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> byCategory(@PathVariable Long categoryId) {
        List<ProductResponse> response = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category products fetched", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PagedResponse<ProductResponse> response = productService.searchProducts(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                inStock,
                page,
                size,
                sortBy,
                sortDir
        );
        return ResponseEntity.ok(ApiResponse.success("Search results fetched", response));
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> uploadImages(
            @PathVariable Long productId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication auth
    ) {
        List<MultipartFile> uploadFiles = files;
        if ((uploadFiles == null || uploadFiles.isEmpty()) && file != null) {
            uploadFiles = List.of(file);
        }
        List<ProductImageResponse> response = productService.uploadProductImages(productId, auth.getName(), uploadFiles);
        return ResponseEntity.ok(ApiResponse.success("Product images uploaded successfully", response));
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImages(@PathVariable Long productId) {
        List<ProductImageResponse> response = productService.getProductImages(productId);
        return ResponseEntity.ok(ApiResponse.success("Product images fetched", response));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            Authentication auth
    ) {
        productService.deleteProductImage(productId, imageId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Product image deleted successfully", null));
    }

    @PatchMapping("/{id}/low-stock-threshold")
    public ResponseEntity<ApiResponse<ProductResponse>> updateLowStockThreshold(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLowStockThresholdRequest request,
            Authentication auth
    ) {
        ProductResponse response = productService.updateLowStockThreshold(id, auth.getName(), request.getLowStockThreshold());
        return ResponseEntity.ok(ApiResponse.success("Low stock threshold updated", response));
    }
}
