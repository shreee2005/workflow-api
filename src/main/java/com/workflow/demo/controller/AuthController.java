package com.workflow.demo.controller;
import com.workflow.demo.entity.User;
import com.workflow.demo.repository.UserRepository;
import com.workflow.demo.security.JwtUtil;
import com.workflow.demo.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AuthController - handles signup, login, and password management.
 *
 * Endpoints:
 *  POST /auth/signup        -> create local user (email + password)
 *  POST /auth/login         -> authenticate with email+password and receive app JWT
 *  POST /auth/set-password  -> set/replace password for authenticated user (Bearer token required)
 *  POST /auth/link-password -> placeholder for token-based linking (not implemented here)
 *
 * Notes:
 * - OAuth/OIDC login is handled separately (OAuth2 success handler links/creates users).
 * - This controller expects JwtUtil to generate/parse tokens.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthController(UserService userService,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    // --- DTOs (records) ---
    public record SignupDto(String email, String password, String name) {}
    public record LoginDto(String email, String password) {}
    public record SetPasswordDto(String currentPassword, String newPassword) {}
    public record LinkPasswordDto(String email, String verificationToken, String newPassword) {}

    // -----------------------
    // SIGNUP
    // -----------------------
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupDto dto) {
        if (dto == null || dto.email() == null || dto.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_and_password_required"));
        }

        try {
            User created = userService.createUser(dto.email().trim().toLowerCase(), dto.password(), dto.name());
            return ResponseEntity.status(201).body(Map.of(
                    "id", created.getId(),
                    "email", created.getEmail()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error"));
        }
    }

    // -----------------------
    // LOGIN
    // -----------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto) {
        if (dto == null || dto.email() == null || dto.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_and_password_required"));
        }

        Optional<User> opt = userService.findByEmail(dto.email().trim().toLowerCase());
        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        User user = opt.get();

        // If no local password set (OAuth-only account), instruct user to use OAuth or set-password
        if (user.getPassword() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "no_local_password_set", "message", "use OAuth login or set a password via /auth/set-password after signing in"));
        }

        boolean ok = passwordEncoder.matches(dto.password(), user.getPassword());
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }

        String token = jwtUtil.generateToken(user.getId().toString(), user.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    // -----------------------
    // SET PASSWORD (authenticated)
    // -----------------------
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestHeader(name = "Authorization", required = false) String authorization,
                                         @RequestBody SetPasswordDto dto) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "missing_authorization"));
        }
        if (dto == null || dto.newPassword() == null || dto.newPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "new_password_required"));
        }

        String token = authorization.substring("Bearer ".length()).trim();

        String userId;
        try {
            Jws<Claims> parsed = jwtUtil.parseClaims(token);
            userId = parsed.getBody().getSubject();
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", "invalid_user_id_in_token"));
        }

        Optional<User> opt = userRepository.findById(uid);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "user_not_found"));
        }

        User user = opt.get();

        // If user had an existing password and provided currentPassword, verify it
        if (user.getPassword() != null && dto.currentPassword() != null && !dto.currentPassword().isBlank()) {
            if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "current_password_incorrect"));
            }
        }

        // Set new password (hash)
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "password_set"));
    }

    // -----------------------
    // LINK PASSWORD (placeholder)
    // -----------------------
    @PostMapping("/link-password")
    public ResponseEntity<?> linkPassword(@RequestBody LinkPasswordDto dto) {
        // For production implement:
        // - Validate verificationToken (signed token or lookup in DB)
        // - Confirm dto.email matches token
        // - Set password for that account
        // Here we return 501 Not Implemented so it's explicit.
        return ResponseEntity.status(501).body(Map.of("error", "not_implemented", "message", "Use /set-password for authenticated flows; implement email-token flow for linking"));
    }
}

