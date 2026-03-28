package com.revshop.service.impl;

import com.revshop.dao.UserDAO;
import com.revshop.dto.LoginRequestDTO;
import com.revshop.dto.LoginResponseDTO;
import com.revshop.dto.RegisterBuyerRequest;
import com.revshop.dto.RegisterSellerRequest;
import com.revshop.dto.UserResponse;
import com.revshop.entity.*;
import com.revshop.exception.BadRequestException;
import com.revshop.exception.ConflictException;
import com.revshop.exception.ResourceNotFoundException;
import com.revshop.mapper.AuthMapper;
import com.revshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.revshop.security.jwt.JwtService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    // ==============================
    // REGISTER BUYER
    // ==============================
    @Override
    @Transactional
    public UserResponse registerBuyer(RegisterBuyerRequest request) {

        log.info("Registering buyer with email={}", request.getEmail());
        validateEmailUniqueness(request.getEmail());

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encryptedPassword)
                .role(Role.BUYER)
                .active(true)
                .build();

        BuyerProfile profile = BuyerProfile.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .user(user)
                .build();

        user.setBuyerProfile(profile);

        User savedUser = userDAO.save(user);
        log.info("Buyer registered successfully with userId={} and email={}", savedUser.getId(), savedUser.getEmail());
        return authMapper.toUserResponse(savedUser);
    }

    // ==============================
    // REGISTER SELLER
    // ==============================
    @Override
    @Transactional
    public UserResponse registerSeller(RegisterSellerRequest request) {

        log.info("Registering seller with email={}", request.getEmail());
        validateEmailUniqueness(request.getEmail());

        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encryptedPassword)
                .role(Role.SELLER)
                .active(true)
                .build();

        SellerProfile profile = SellerProfile.builder()
                .businessName(request.getBusinessName())
                .gstNumber(request.getGstNumber())
                .phone(request.getPhone())
                .businessAddress(request.getBusinessAddress())
                .user(user)
                .build();

        user.setSellerProfile(profile);

        User savedUser = userDAO.save(user);
        log.info("Seller registered successfully with userId={} and email={}", savedUser.getId(), savedUser.getEmail());
        return authMapper.toUserResponse(savedUser);
    }

    // ==============================
    // LOGIN
    // ==============================
    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {

        log.info("Authenticating user with email={}", request.getEmail());
        User user = userDAO.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed due to invalid password for email={}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.getActive()) {
            log.warn("Login blocked because user is disabled for email={}", request.getEmail());
            throw new BadRequestException("User is disabled");
        }

        // ✅ GENERATE JWT
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        log.info("Authentication successful for email={} with role={}", user.getEmail(), user.getRole());

        return LoginResponseDTO.builder()
                .token(token)
                .role(user.getRole().name())
                .build();
    }
    // ==============================
    // FIND USER BY EMAIL
    // ==============================
    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return authMapper.toUserResponse(user);
    }

    // ==============================
    // PRIVATE HELPERS
    // ==============================
    private void validateEmailUniqueness(String email) {
        log.debug("Checking email uniqueness for {}", email);
        if (userDAO.existsByEmail(email)) {
            log.warn("Registration blocked because email already exists: {}", email);
            throw new ConflictException("Email already registered: " + email);
        }
    }
}
