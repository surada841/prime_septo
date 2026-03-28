package com.revshop.mapper;

import com.revshop.entity.Product;
import com.revshop.entity.ProductImage;
import com.revshop.dto.product.ProductResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        List<String> imageUrls = product.getImages() == null ? List.of() :
                product.getImages()
                        .stream()
                        .sorted(Comparator.comparing(ProductImage::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                        .map(ProductImage::getImageUrl)
                        .toList();

        BigDecimal effectiveDiscounted = product.getDiscountedPrice() == null ? product.getPrice() : product.getDiscountedPrice();
        BigDecimal effectiveMrp = product.getMrpPrice() == null ? effectiveDiscounted : product.getMrpPrice();
        Integer effectiveLowStockThreshold = product.getLowStockThreshold() == null ? 5 : product.getLowStockThreshold();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .mrpPrice(effectiveMrp)
                .discountedPrice(effectiveDiscounted)
                .stock(product.getStock())
                .lowStockThreshold(effectiveLowStockThreshold)
                .inStock(product.getInStock())
                .active(product.getActive())
                .status(product.getStatus())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .sellerId(product.getSeller().getId())
                .sellerEmail(product.getSeller().getEmail())
                .imageUrls(imageUrls)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
