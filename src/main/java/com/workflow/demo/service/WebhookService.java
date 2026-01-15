// WebhookService.java
package com.workflow.demo.service;

import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WebhookService {

    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    public WebhookService(IncomingEventRepository incomingEventRepository,
                          JobPublisher jobPublisher, WorkflowRunService workflowRunService) {
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
    }

    public void acceptWebhook(UUID workflowId, Map<String, Object> body, String idempotencyKey) {
        // optional dedupe
        if (idempotencyKey != null && incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        IncomingEvent ev = new IncomingEvent();
        ev.setWorkflowId(workflowId);
        ev.setPayload(toJson(body)); // store JSON string in DB
        ev.setIdempotencyKey(idempotencyKey);
        ev.setReceivedAt(OffsetDateTime.now());

        incomingEventRepository.save(ev);
        WorkflowRun run = workflowRunService.createQueuedRun(workflowId, ev.getId());
        // publish a job message for worker
        jobPublisher.publishRun(run.getId(), ev.getId(), workflowId, body.toString());
    }

    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
