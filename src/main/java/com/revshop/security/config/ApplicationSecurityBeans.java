package com.revshop.security.config;

import com.revshop.dao.UserDAO;
import com.revshop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationSecurityBeans {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder; // injected from PasswordConfig

    // ===============================
    // USER DETAILS SERVICE
    // ===============================
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userDAO.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return org.springframework.security.core.userdetails.User
                    .builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
        };
    }

    // ===============================
    // AUTH PROVIDER
    // ===============================
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService());

        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}