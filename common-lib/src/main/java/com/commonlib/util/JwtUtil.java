package com.commonlib.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 256-bit secret key (Base64-encoded) — generated securely
    private static final String SECRET_KEY = "u8X5V6uQGfY2XtF9fRt3JjW0dK7aV9f1H2z8Qx0Rk3A=";

    // Convert Base64 string to a Key object
    private Key getSigningKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extract username (subject) from token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Generate a JWT token
    public String generateToken(String username) {
        long expirationTime = 1000 * 60 * 60 * 10; // 10 hours

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            return false; // invalid signature, malformed token, etc.
        }
    }

    // Check expiration
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    // Extract all claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
