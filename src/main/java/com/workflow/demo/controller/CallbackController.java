package com.workflow.demo.controller;

import com.workflow.demo.service.WorkflowResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/hooks/callback")
public class CallbackController {

    private final WorkflowResumeService workflowResumeService;

    public CallbackController(WorkflowResumeService workflowResumeService) {
        this.workflowResumeService = workflowResumeService;
    }

    @PostMapping("/{correlationId}")
    public ResponseEntity<?> callback(
            @PathVariable String correlationId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            workflowResumeService.resumeByCorrelationId(correlationId, body);
            return ResponseEntity.accepted().body(Map.of("status", "resumed"));
        } catch (RuntimeException ex) {
            if ("WAIT_STATE_NOT_FOUND".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", "Wait state not found"));
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal error"));
        }
    }
}