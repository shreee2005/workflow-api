package com.workflow.demo.controller;
import com.workflow.demo.service.WebhookService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/hooks")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{workflowId}")
    public ResponseEntity<?> receive(@PathVariable UUID workflowId,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                     @RequestBody Map<String,Object> body) {
        webhookService.acceptWebhook(workflowId, body, idempotencyKey);
        return ResponseEntity.accepted().body(Map.of("status","queued"));
    }
}

