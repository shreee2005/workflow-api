package com.workflow.demo.controller;

import com.workflow.demo.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/hooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{workflowId}")
    public ResponseEntity<?> receive(
            @PathVariable UUID workflowId,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            @RequestBody Map<String, Object> body
    ) {
        try {

            webhookService.acceptWebhook(
                    workflowId,
                    body,
                    idempotencyKey
            );

            return ResponseEntity
                    .accepted()
                    .body(Map.of("status", "queued"));

        } catch (RuntimeException ex) {

            if ("WORKFLOW_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity
                        .status(404)
                        .body(Map.of("error", "Workflow not found"));
            }

            if ("WORKFLOW_NOT_ACTIVE".equals(ex.getMessage())) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("error", "Workflow is not active"));
            }

            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }
}