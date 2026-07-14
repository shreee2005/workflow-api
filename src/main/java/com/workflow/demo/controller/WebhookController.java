package com.workflow.demo.controller;

import com.workflow.demo.service.WebhookService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/hooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final Tracer tracer;

    public WebhookController(WebhookService webhookService, Tracer tracer) {
        this.webhookService = webhookService;
        this.tracer = tracer;
    }

    @PostMapping("/{workflowId}")
    public ResponseEntity<?> receive(
            @PathVariable UUID workflowId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody Map<String, Object> body
    ) {
        Span span = tracer.spanBuilder("webhook.receive").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Add custom span attributes
            span.setAttribute("workflow.id", workflowId.toString());
            if (idempotencyKey != null) {
                span.setAttribute("idempotency.key", idempotencyKey);
            }
            span.setAttribute("webhook.payload.size", body.size());

            webhookService.acceptWebhook(workflowId, body, idempotencyKey);

            return ResponseEntity
                    .accepted()
                    .body(Map.of("status", "queued"));

        } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setAttribute("error", true);

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
        } finally {
            span.end();
        }
    }
}