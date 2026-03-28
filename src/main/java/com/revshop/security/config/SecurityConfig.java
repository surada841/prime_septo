package com.revshop.security.config;

import com.revshop.security.jwt.JwtAuthFilter;
import com.revshop.security.jwt.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // enables @PreAuthorize
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Initializing Spring Security filter chain for RevShop");
        http
                // Disable CSRF for REST APIs
                .csrf(csrf -> csrf.disable())

                // No sessions (JWT only)
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Unauthorized handler
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                //  API Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        //  PUBLIC APIs
                        .requestMatchers(
                                "/api/auth/**",
                                "/error",
                                "/uploads/**",
                                "/",
                                "/home",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/buyer/**",
                                "/seller/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",

                                // Swagger (optional)
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        //  EVERYTHING ELSE NEEDS JWT
                        .anyRequest().authenticated()
                )

                //  Auth Provider
                .authenticationProvider(authenticationProvider)

                //  JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
