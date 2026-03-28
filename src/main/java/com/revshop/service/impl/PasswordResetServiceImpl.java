package com.revshop.service.impl;

import com.revshop.dao.PasswordResetTokenDAO;
import com.revshop.dao.UserDAO;
import com.revshop.dto.password.ForgotPasswordResponse;
import com.revshop.entity.PasswordResetToken;
import com.revshop.entity.User;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private final UserDAO userDAO;
    private final PasswordResetTokenDAO passwordResetTokenDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ForgotPasswordResponse generateResetToken(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        passwordResetTokenDAO.deactivateActiveTokensByUser(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        String token = UUID.randomUUID().toString().replace("-", "");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .active(true)
                .build();
        passwordResetTokenDAO.save(resetToken);

        return ForgotPasswordResponse.builder()
                .email(user.getEmail())
                .resetToken(token)
                .expiresAt(expiresAt)
                .note("Mock flow: in production this token is sent via email.")
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenDAO.findActiveByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or inactive reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetToken.setActive(false);
            passwordResetTokenDAO.save(resetToken);
            throw new BadRequestException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userDAO.update(user);

        resetToken.setUsed(true);
        resetToken.setActive(false);
        passwordResetTokenDAO.save(resetToken);
    }
}
