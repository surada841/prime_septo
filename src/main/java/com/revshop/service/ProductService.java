package com.revshop.service;

import com.revshop.dto.common.PagedResponse;
import com.revshop.dto.product.ProductCreateRequest;
import com.revshop.dto.product.ProductImageResponse;
import com.revshop.dto.product.ProductResponse;
import com.revshop.dto.product.ProductUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request, String sellerEmail);

    ProductResponse updateProduct(Long productId, String sellerEmail, ProductUpdateRequest request);

    ProductResponse updateLowStockThreshold(Long productId, String sellerEmail, Integer lowStockThreshold);

    void deleteProduct(Long productId, String sellerEmail);

    List<ProductResponse> getSellerProducts(String sellerEmail);

    List<ProductResponse> getMyLowStockProducts(String sellerEmail);

    List<ProductResponse> getAllActiveProducts();

    ProductResponse getProductById(Long id);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> searchProducts(String keyword);

    PagedResponse<ProductResponse> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    List<ProductImageResponse> uploadProductImages(Long productId, String sellerEmail, List<MultipartFile> files);

    List<ProductImageResponse> getProductImages(Long productId);

    void deleteProductImage(Long productId, Long imageId, String sellerEmail);
}
