package com.revshop.dao.impl;

import com.revshop.dao.ProductImageDAO;
import com.revshop.entity.ProductImage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductImageDAOImpl implements ProductImageDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ProductImage save(ProductImage image) {
        if (image.getId() == null) {
            em.persist(image);
            return image;
        }
        return em.merge(image);
    }

    @Override
    public List<ProductImage> findByProductId(Long productId) {
        return em.createQuery("""
                SELECT i FROM ProductImage i
                WHERE i.product.id = :productId
                ORDER BY i.displayOrder ASC
                """, ProductImage.class)
                .setParameter("productId", productId)
                .getResultList();
    }

    @Override
    public Optional<ProductImage> findById(Long imageId) {
        return Optional.ofNullable(em.find(ProductImage.class, imageId));
    }

    @Override
    public long countByProductId(Long productId) {
        Long count = em.createQuery("""
                SELECT COUNT(i) FROM ProductImage i
                WHERE i.product.id = :productId
                """, Long.class)
                .setParameter("productId", productId)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public void delete(ProductImage image) {
        ProductImage managed = em.contains(image) ? image : em.merge(image);
        em.remove(managed);
    }

    @Override
    public void deleteByProductId(Long productId) {
        em.createQuery("""
                DELETE FROM ProductImage i
                WHERE i.product.id = :productId
                """)
                .setParameter("productId", productId)
                .executeUpdate();
    }
}
