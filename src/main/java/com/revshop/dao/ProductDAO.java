package com.revshop.dao;

import com.revshop.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductDAO {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findBySellerEmail(String email);

    List<Product> findByCategory(Long categoryId);

    List<Product> findActiveProducts();

    List<Product> searchByName(String keyword);

    List<Product> searchPublicProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            String sortBy,
            String sortDir,
            int offset,
            int limit
    );

    long countPublicProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock
    );

    long countBySellerEmail(String sellerEmail);

    long countActiveBySellerEmail(String sellerEmail);

    long countLowStockBySellerEmail(String sellerEmail, int threshold);

    List<Product> findLowStockBySellerEmail(String sellerEmail);

    long countActiveByCategoryId(Long categoryId);
}
