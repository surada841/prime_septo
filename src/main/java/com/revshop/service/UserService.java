package com.revshop.service;

import com.revshop.dto.LoginRequestDTO;
import com.revshop.dto.LoginResponseDTO;
import com.revshop.dto.RegisterBuyerRequest;
import com.revshop.dto.RegisterSellerRequest;
import com.revshop.dto.UserResponse;

public interface UserService {

    // ==============================
    // REGISTER BUYER
    // ==============================
    UserResponse registerBuyer(RegisterBuyerRequest request);

    // ==============================
    // REGISTER SELLER
    // ==============================
    UserResponse registerSeller(RegisterSellerRequest request);

    // ==============================
    // LOGIN
    // ==============================
    LoginResponseDTO login(LoginRequestDTO request);

    // ==============================
    // FIND USER
    // ==============================
    UserResponse getByEmail(String email);
}
