package com.revshop.dao.impl;

import com.revshop.dao.UserDAO;
import com.revshop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDAOImpl implements UserDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public User save(User user) {
        entityManager.persist(user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false",
                User.class
        );
        query.setParameter("id", id);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            TypedQuery<User> query = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false",
                    User.class
            );
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email AND u.isDeleted = false",
                Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }

    @Override
    public void update(User user) {
        entityManager.merge(user);
    }

    @Override
    public void delete(Long id) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            user.setActive(false);
            user.setIsDeleted(true); // soft delete
            entityManager.merge(user);
        }
    }
}
