package com.revshop.dao.impl;

import com.revshop.dao.AdminDAO;
import com.revshop.entity.Role;
import com.revshop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminDAOImpl implements AdminDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long countUsers(Boolean includeDeleted) {
        String jpql = includeDeleted != null && includeDeleted
                ? "SELECT COUNT(u) FROM User u"
                : "SELECT COUNT(u) FROM User u WHERE u.isDeleted = false";
        Long count = em.createQuery(jpql, Long.class).getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countUsersByRole(Role role, Boolean includeDeleted) {
        String jpql = includeDeleted != null && includeDeleted
                ? "SELECT COUNT(u) FROM User u WHERE u.role = :role"
                : "SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isDeleted = false";
        Long count = em.createQuery(jpql, Long.class)
                .setParameter("role", role)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countUsersByActive(boolean active, Boolean includeDeleted) {
        String jpql = includeDeleted != null && includeDeleted
                ? "SELECT COUNT(u) FROM User u WHERE u.active = :active"
                : "SELECT COUNT(u) FROM User u WHERE u.active = :active AND u.isDeleted = false";
        Long count = em.createQuery(jpql, Long.class)
                .setParameter("active", active)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countDeletedUsers() {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.isDeleted = true", Long.class)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countProducts() {
        Long count = em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.isDeleted = false", Long.class)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countOrders() {
        Long count = em.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.isDeleted = false", Long.class)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public long countPayments() {
        Long count = em.createQuery("SELECT COUNT(p) FROM Payment p WHERE p.isDeleted = false", Long.class)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    @Override
    public List<User> searchUsers(String keyword, Role role, Boolean active, int offset, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        List<String> clauses = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            clauses.add("LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))");
        }
        if (role != null) {
            clauses.add("u.role = :role");
        }
        if (active != null) {
            clauses.add("u.active = :active");
        }

        for (String clause : clauses) {
            jpql.append(" AND ").append(clause);
        }

        jpql.append(" ORDER BY u.createdAt DESC");
        TypedQuery<User> query = em.createQuery(jpql.toString(), User.class);

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (role != null) {
            query.setParameter("role", role);
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        return query.setFirstResult(Math.max(0, offset))
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }

    @Override
    public long countSearchUsers(String keyword, Role role, Boolean active) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(u) FROM User u WHERE 1=1");
        List<String> clauses = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            clauses.add("LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))");
        }
        if (role != null) {
            clauses.add("u.role = :role");
        }
        if (active != null) {
            clauses.add("u.active = :active");
        }

        for (String clause : clauses) {
            jpql.append(" AND ").append(clause);
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", keyword);
        }
        if (role != null) {
            query.setParameter("role", role);
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        Long total = query.getSingleResult();
        return total == null ? 0 : total;
    }

    @Override
    public Optional<User> findUserByIdAnyState(Long userId) {
        return Optional.ofNullable(em.find(User.class, userId));
    }

    @Override
    public User saveUser(User user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        }
        return em.merge(user);
    }
}
