package com.revshop.dao.impl;

import com.revshop.dao.ProductDAO;
import com.revshop.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductDAOImpl implements ProductDAO {

    private final EntityManager em;

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            em.persist(product);
            return product;
        }
        return em.merge(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE p.id = :id
                AND p.isDeleted = false
                """, Product.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Product> findBySellerEmail(String email) {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE p.seller.email = :email
                AND p.active = true
                AND p.isDeleted = false
                """, Product.class)
                .setParameter("email", email)
                .getResultList();
    }

    @Override
    public List<Product> findByCategory(Long categoryId) {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE p.category.id = :id
                AND p.active = true
                AND p.isDeleted = false
                """, Product.class)
                .setParameter("id", categoryId)
                .getResultList();
    }

    @Override
    public List<Product> findActiveProducts() {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE p.active = true
                AND p.isDeleted = false
                """, Product.class).getResultList();
    }

    @Override
    public List<Product> searchByName(String keyword) {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :k, '%'))
                AND p.active = true
                AND p.isDeleted = false
                """, Product.class)
                .setParameter("k", keyword)
                .getResultList();
    }

    @Override
    public List<Product> searchPublicProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            String sortBy,
            String sortDir,
            int offset,
            int limit
    ) {
        String orderByExpr = switch (sortBy) {
            case "name" -> "LOWER(p.name)";
            case "price" -> "p.price";
            case "createdAt" -> "p.createdAt";
            default -> "p.createdAt";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder jpql = new StringBuilder("""
                SELECT p FROM Product p
                WHERE p.active = true
                AND p.isDeleted = false
                """);
        List<String> clauses = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            clauses.add("LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))");
        }
        if (categoryId != null) {
            clauses.add("p.category.id = :categoryId");
        }
        if (minPrice != null) {
            clauses.add("p.price >= :minPrice");
        }
        if (maxPrice != null) {
            clauses.add("p.price <= :maxPrice");
        }
        if (inStock != null) {
            clauses.add("p.inStock = :inStock");
        }

        for (String clause : clauses) {
            jpql.append(" AND ").append(clause);
        }
        jpql.append(" ORDER BY ").append(orderByExpr).append(" ").append(direction);

        TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (inStock != null) {
            query.setParameter("inStock", inStock);
        }

        return query.setFirstResult(Math.max(0, offset))
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }

    @Override
    public long countPublicProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock
    ) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(p) FROM Product p
                WHERE p.active = true
                AND p.isDeleted = false
                """);
        List<String> clauses = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            clauses.add("LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))");
        }
        if (categoryId != null) {
            clauses.add("p.category.id = :categoryId");
        }
        if (minPrice != null) {
            clauses.add("p.price >= :minPrice");
        }
        if (maxPrice != null) {
            clauses.add("p.price <= :maxPrice");
        }
        if (inStock != null) {
            clauses.add("p.inStock = :inStock");
        }

        for (String clause : clauses) {
            jpql.append(" AND ").append(clause);
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (inStock != null) {
            query.setParameter("inStock", inStock);
        }

        Long total = query.getSingleResult();
        return total == null ? 0 : total;
    }

    @Override
    public long countBySellerEmail(String sellerEmail) {
        Long count = em.createQuery("""
                SELECT COUNT(p) FROM Product p
                WHERE p.seller.email = :sellerEmail
                AND p.isDeleted = false
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countActiveBySellerEmail(String sellerEmail) {
        Long count = em.createQuery("""
                SELECT COUNT(p) FROM Product p
                WHERE p.seller.email = :sellerEmail
                AND p.active = true
                AND p.isDeleted = false
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countLowStockBySellerEmail(String sellerEmail, int threshold) {
        Long count = em.createQuery("""
                SELECT COUNT(p) FROM Product p
                WHERE p.seller.email = :sellerEmail
                AND p.active = true
                AND p.isDeleted = false
                AND p.stock <= :threshold
                """, Long.class)
                .setParameter("sellerEmail", sellerEmail)
                .setParameter("threshold", threshold)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public List<Product> findLowStockBySellerEmail(String sellerEmail) {
        return em.createQuery("""
                SELECT p FROM Product p
                WHERE p.seller.email = :sellerEmail
                AND p.active = true
                AND p.isDeleted = false
                AND p.stock <= COALESCE(p.lowStockThreshold, 5)
                ORDER BY p.stock ASC, p.updatedAt DESC
                """, Product.class)
                .setParameter("sellerEmail", sellerEmail)
                .getResultList();
    }

    @Override
    public long countActiveByCategoryId(Long categoryId) {
        Long count = em.createQuery("""
                SELECT COUNT(p) FROM Product p
                WHERE p.category.id = :categoryId
                AND p.active = true
                AND p.isDeleted = false
                """, Long.class)
                .setParameter("categoryId", categoryId)
                .getSingleResult();
        return count == null ? 0 : count;
    }
}
