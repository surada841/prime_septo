package com.revshop.dto.password;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ForgotPasswordResponse {

    private String email;
    private String resetToken;
    private LocalDateTime expiresAt;
    private String note;
}
