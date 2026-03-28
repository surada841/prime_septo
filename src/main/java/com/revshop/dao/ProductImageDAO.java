package com.revshop.dao;

import com.revshop.entity.ProductImage;

import java.util.List;
import java.util.Optional;

public interface ProductImageDAO {

    ProductImage save(ProductImage image);

    List<ProductImage> findByProductId(Long productId);

    Optional<ProductImage> findById(Long imageId);

    long countByProductId(Long productId);

    void delete(ProductImage image);

    void deleteByProductId(Long productId);
}
