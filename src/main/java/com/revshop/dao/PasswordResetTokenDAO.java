package com.revshop.dao;

import com.revshop.entity.PasswordResetToken;
import com.revshop.entity.User;

import java.util.Optional;

public interface PasswordResetTokenDAO {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findActiveByToken(String token);

    void deactivateActiveTokensByUser(User user);
}
