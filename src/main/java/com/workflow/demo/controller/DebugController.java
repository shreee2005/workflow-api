package com.workflow.demo.controller;
import com.workflow.demo.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final JwtUtil jwtUtil;

    public DebugController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestHeader(name = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(400).body(Map.of("error", "missing_authorization"));
        }
        String token = auth.substring("Bearer ".length()).trim();
        try {
            Jws<Claims> parsed = jwtUtil.parseClaims(token);
            return ResponseEntity.ok(Map.of(
                    "subject", parsed.getBody().getSubject(),
                    "claims", parsed.getBody()
            ));
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "message", e.getMessage()));
        }
    }
}

