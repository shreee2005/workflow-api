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
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            secret = "dev-secret-please-change-to-a-long-random-value-303957762";
        }

        if (secret.length() < 32) {
            secret = (secret + "00000000000000000000000000000000").substring(0, 32);
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

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

    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
