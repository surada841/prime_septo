package com.revshop.dao.impl;

import com.revshop.dao.CategoryDAO;
import com.revshop.entity.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryDAOImpl implements CategoryDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return em.createQuery("""
                SELECT c FROM Category c
                WHERE c.id = :id
                AND c.active = true
                AND c.isDeleted = false
                """, Category.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return em.createQuery("""
                SELECT c FROM Category c
                WHERE LOWER(TRIM(c.slug)) = LOWER(TRIM(:slug))
                """, Category.class)
                .setParameter("slug", slug)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Category> findAnyByName(String name) {
        return em.createQuery("""
                SELECT c FROM Category c
                WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
                ORDER BY c.id DESC
                """, Category.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Category> findAllActive() {
        return em.createQuery("""
                SELECT c FROM Category c
                WHERE c.active = true
                AND c.isDeleted = false
                ORDER BY c.name ASC
                """, Category.class)
                .getResultList();
    }

    @Override
    public List<Category> findAllActiveWithParent() {
        return em.createQuery("""
                SELECT c FROM Category c
                LEFT JOIN FETCH c.parent p
                WHERE c.active = true
                AND c.isDeleted = false
                ORDER BY c.name ASC
                """, Category.class)
                .getResultList();
    }

    @Override
    public boolean existsByName(String name) {
        Long count = em.createQuery("""
                SELECT COUNT(c) FROM Category c
                WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
                AND c.isDeleted = false
                """, Long.class)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public long countActiveChildren(Long parentId) {
        Long count = em.createQuery("""
                SELECT COUNT(c) FROM Category c
                WHERE c.parent.id = :parentId
                AND c.active = true
                AND c.isDeleted = false
                """, Long.class)
                .setParameter("parentId", parentId)
                .getSingleResult();
        return count == null ? 0 : count;
    }
}
