package com.workflow.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    // 24 hours
    private final long validityMs = 1000L * 60 * 60 * 24;

    private final Key key;

    public JwtUtil() {
        // Prefer environment variable JWT_SECRET. Must be at least 32 bytes for HS256.
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            // fallback dev secret (make sure to override in prod / CI)
            secret = "dev-secret-please-change-to-a-long-random-value-303957762";
        }

        if (secret.length() < 32) {
            // pad to 32 bytes deterministically so key creation never fails in dev
            secret = (secret + "00000000000000000000000000000000").substring(0, 32);
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // DEBUG (optional): uncomment if you still want to see what is used at runtime
        // System.out.println("=== JWT_SECRET AT RUNTIME ===");
        // System.out.println("length=" + secret.length());
        // System.out.println("value=[" + secret + "]");
        // System.out.println("================================");
    }

    public String generateToken(String userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + validityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        Claims claims = parseClaims(token).getBody();
        return claims.getSubject();
    }
    public UUID validateAndExtractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }
    public Jws<Claims> parseClaims(String token) {
        // parseClaimsJws will throw JwtException on invalid signature/expired etc.
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
