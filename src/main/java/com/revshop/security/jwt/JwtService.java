package com.revshop.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    // ==============================
    // SECRET KEY (Base64 encoded)
    // ==============================
    private static final String SECRET_KEY =
            "TXlTdXBlclNlY3JldEtleUZvclJldlNob3BKU1dUMTIzNDU2";

    // ==============================
    // TOKEN GENERATION
    // ==============================
    public String generateToken(String email, String role) {
        log.debug("Generating JWT token for email={} and role={}", email, role);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
                .signWith(getSigningKey())
                .compact();
    }

    // ==============================
    // EXTRACT EMAIL
    // ==============================
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ==============================
    // EXTRACT ROLE
    // ==============================
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ==============================
    // VALIDATE TOKEN
    // ==============================
    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        boolean valid = extractedEmail.equals(email) && !isTokenExpired(token);
        log.debug("JWT validation result for email={} is {}", email, valid);
        return valid;
    }

    // ==============================
    // INTERNAL HELPERS
    // ==============================
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // jjwt 0.12 style
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ✅ FIXED HERE (SecretKey instead of Key)
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}