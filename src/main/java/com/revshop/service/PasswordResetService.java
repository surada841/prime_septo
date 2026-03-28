package com.revshop.service;

import com.revshop.dto.password.ForgotPasswordResponse;

public interface PasswordResetService {

    ForgotPasswordResponse generateResetToken(String email);

    void resetPassword(String token, String newPassword);
}
