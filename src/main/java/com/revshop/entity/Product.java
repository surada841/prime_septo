package com.revshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= BASIC =================
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    // ================= PRICING =================
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "mrp_price", precision = 10, scale = 2)
    private BigDecimal mrpPrice;

    @Column(name = "discounted_price", precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    // ================= STOCK =================
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    // ================= FLAGS =================
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ================= STATUS =================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    // ================= CATEGORY =================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // ================= SELLER =================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // ================= IMAGES =================
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;
}
