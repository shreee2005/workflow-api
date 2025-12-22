package com.workflow.demo.controller;

import com.workflow.demo.entity.ApiKey;
import com.workflow.demo.entity.Team;
import com.workflow.demo.repository.ApiKeyRepository;
import com.workflow.demo.repository.TeamRepository;
import com.workflow.demo.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams/{teamId}/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepo;
    private final TeamRepository teamRepository;

    public ApiKeyController(ApiKeyService apiKeyService,
                            ApiKeyRepository apiKeyRepo,
                            TeamRepository teamRepository) {
        this.apiKeyService = apiKeyService;
        this.apiKeyRepo = apiKeyRepo;
        this.teamRepository = teamRepository;
    }

    /**
     * Create API key (JWT auth)
     * Body: { "name": "worker-key" }
     * Returns composite key ONCE.
     */
    @PostMapping
    public ResponseEntity<?> createKey(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team_not_found"));

        if (!team.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "not_team_owner"));
        }

        String name = body.getOrDefault("name", "unnamed").trim();
        if (name.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name_required"));
        }

        String composite = apiKeyService.createApiKey(teamId, userId, name);

        return ResponseEntity.status(201).body(Map.of(
                "apiKey", composite,
                "warning", "Save this key now. It will not be shown again."
        ));
    }

    /**
     * List API keys (metadata only, no secret)
     */
    @GetMapping
    public ResponseEntity<?> listKeys(
            @PathVariable UUID teamId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team_not_found"));

        if (!team.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "not_team_owner"));
        }

        List<ApiKey> keys = apiKeyRepo.findByTeamIdAndRevokedFalse(teamId);

        var out = keys.stream()
                .map(k -> Map.<String, Object>of(
                        "id", k.getId(),
                        "name", k.getName(),
                        "createdBy", k.getCreatedBy(),
                        "createdAt", k.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    /**
     * Revoke an API key
     */
    @PostMapping("/{keyId}/revoke")
    public ResponseEntity<?> revokeKey(
            @PathVariable UUID teamId,
            @PathVariable UUID keyId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("team_not_found"));

        if (!team.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "not_team_owner"));
        }

        apiKeyService.revokeKey(keyId);
        return ResponseEntity.noContent().build();
    }

    /* ---------- helpers ---------- */

    private UUID currentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("unauthenticated");
        }
        if (auth.getPrincipal() instanceof UUID id) {
            return id;
        }
        throw new IllegalStateException("unexpected_principal_type");
    }
}
