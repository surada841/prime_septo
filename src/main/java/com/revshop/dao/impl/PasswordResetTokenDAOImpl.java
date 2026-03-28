package com.revshop.dao.impl;

import com.revshop.dao.PasswordResetTokenDAO;
import com.revshop.entity.PasswordResetToken;
import com.revshop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PasswordResetTokenDAOImpl implements PasswordResetTokenDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        if (token.getId() == null) {
            em.persist(token);
            return token;
        }
        return em.merge(token);
    }

    @Override
    public Optional<PasswordResetToken> findActiveByToken(String token) {
        return em.createQuery("""
                SELECT t FROM PasswordResetToken t
                JOIN FETCH t.user u
                WHERE t.token = :token
                AND t.active = true
                AND t.used = false
                AND t.isDeleted = false
                AND u.isDeleted = false
                """, PasswordResetToken.class)
                .setParameter("token", token)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void deactivateActiveTokensByUser(User user) {
        em.createQuery("""
                UPDATE PasswordResetToken t
                SET t.active = false
                WHERE t.user = :user
                AND t.active = true
                AND t.used = false
                AND t.isDeleted = false
                """)
                .setParameter("user", user)
                .executeUpdate();
    }
}
