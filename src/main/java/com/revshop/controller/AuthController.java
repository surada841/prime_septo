package com.revshop.controller;

import com.revshop.dto.common.ApiResponse;
import com.revshop.dto.LoginRequestDTO;
import com.revshop.dto.LoginResponseDTO;
import com.revshop.dto.RegisterBuyerRequest;
import com.revshop.dto.RegisterSellerRequest;
import com.revshop.dto.UserResponse;
import com.revshop.dto.password.ForgotPasswordRequest;
import com.revshop.dto.password.ForgotPasswordResponse;
import com.revshop.dto.password.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.revshop.service.PasswordResetService;
import com.revshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Authentication and registration APIs")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register/buyer")
    public ResponseEntity<ApiResponse<UserResponse>> registerBuyer(
            @Valid @RequestBody RegisterBuyerRequest request) {
        log.info("Buyer registration requested for email={}", request.getEmail());
        UserResponse userResponse = userService.registerBuyer(request);
        log.info("Buyer registration completed for email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Buyer registered successfully", userResponse));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<ApiResponse<UserResponse>> registerSeller(
            @Valid @RequestBody RegisterSellerRequest request) {
        log.info("Seller registration requested for email={}", request.getEmail());
        UserResponse userResponse = userService.registerSeller(request);
        log.info("Seller registration completed for email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Seller registered successfully", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        log.info("Login requested for email={}", request.getEmail());
        LoginResponseDTO loginResponse = userService.login(request);
        log.info("Login completed for email={} with role={}", request.getEmail(), loginResponse.getRole());
        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        log.info("Forgot password requested for email={}", request.getEmail());
        ForgotPasswordResponse response = passwordResetService.generateResetToken(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset token generated", response));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        log.info("Reset password requested for token={}", request.getToken());
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        log.info("Password reset completed for token={}", request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }
}
