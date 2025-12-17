package com.workflow.demo.controller;
import com.workflow.demo.entity.ApiKey;
import com.workflow.demo.service.ApiKeyService;
import com.workflow.demo.repository.ApiKeyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams/{teamId}/keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepo;

    public ApiKeyController(ApiKeyService apiKeyService, ApiKeyRepository apiKeyRepo) {
        this.apiKeyService = apiKeyService;
        this.apiKeyRepo = apiKeyRepo;
    }

    // Create — returns composite "<keyId>:<secret>" (show once)
    @PostMapping
    public ResponseEntity<Map<String, String>> createKey(@PathVariable UUID teamId,
                                                         @RequestBody Map<String, String> body,
                                                         @RequestHeader("X-User-Id") String createdByStr) {
        UUID createdBy = UUID.fromString(createdByStr); // adapt how you extract current user id
        String name = body.getOrDefault("name", "unnamed");
        String composite = apiKeyService.createApiKey(teamId, createdBy, name);
        // RESP: show secret only once and warn in UI
        return ResponseEntity.status(201).body(Map.of(
                "apiKey", composite,
                "warning", "Save this secret now — it will not be shown again."
        ));
    }

    // List keys (metadata only; no secret)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listKeys(@PathVariable UUID teamId) {
        List<ApiKey> keys = apiKeyRepo.findByTeamIdAndRevokedFalse(teamId);
        var out = keys.stream()
                .map(k -> Map.<String,Object>of(
                        "id", k.getId(),
                        "name", k.getName(),
                        "createdBy", k.getCreatedBy(),
                        "createdAt", k.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    // Revoke a key
    @PostMapping("/{keyId}/revoke")
    public ResponseEntity<?> revoke(@PathVariable UUID teamId, @PathVariable UUID keyId) {
        // optional: verify key belongs to team and caller is admin
        apiKeyService.revokeKey(keyId);
        return ResponseEntity.noContent().build();
    }
}

