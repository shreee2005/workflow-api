package com.workflow.demo.service;
import com.workflow.demo.entity.ApiKey;
import com.workflow.demo.entity.Team;
import com.workflow.demo.repository.ApiKeyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepo;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepo, PasswordEncoder passwordEncoder) {
        this.apiKeyRepo = apiKeyRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create API key: generate id, secret, persist hash, return composite keyId:secret.
     *
     * Caller MUST show returned string to user exactly once.
     */
    public String createApiKey(UUID teamId, UUID createdBy, String name) {
        // generate secret
        String secret = generateSecret();
        // generate key id
        UUID keyId = UUID.randomUUID();

        ApiKey k = new ApiKey();
        k.setId(keyId);

        Team stubTeam = new Team();
        stubTeam.setId(teamId); // avoid extra DB query; JPA will save reference id
        k.setTeam(stubTeam);

        k.setName(name);
        k.setCreatedBy(createdBy);
        k.setSecretHash(passwordEncoder.encode(secret));
        k.setRevoked(false);

        apiKeyRepo.save(k); // persisted with id and secretHash only

        // return composite token once
        return keyId.toString() + ":" + secret;
    }

    /**
     * Validate a composite token "<keyId>:<secret>" for the given team.
     */
    public boolean validateApiKey(UUID teamId, String composite) {
        if (composite == null) return false;
        String[] parts = composite.split(":", 2);
        if (parts.length != 2) return false;
        UUID keyId;
        try { keyId = UUID.fromString(parts[0]); } catch (IllegalArgumentException e) { return false; }
        String secret = parts[1];

        return apiKeyRepo.findById(keyId)
                .filter(k -> !k.isRevoked())
                .filter(k -> k.getTeam() != null && teamId.equals(k.getTeam().getId()))
                .map(k -> passwordEncoder.matches(secret, k.getSecretHash()))
                .orElse(false);
    }

    private String generateSecret() {
        byte[] random = new byte[32];
        ThreadLocalRandom.current().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public void revokeKey(UUID keyId) {
        apiKeyRepo.findById(keyId).ifPresent(k -> {
            k.setRevoked(true);
            apiKeyRepo.save(k);
        });
    }
}


