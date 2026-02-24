// WebhookService.java
package com.workflow.demo.service;

import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WebhookService {

    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    private final WorkflowRunRepository workflowRunRepository;
    public WebhookService(IncomingEventRepository incomingEventRepository,
                          JobPublisher jobPublisher, WorkflowRunService workflowRunService, WorkflowRunRepository workflowRunRepository) {
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
        this.workflowRunRepository = workflowRunRepository;
    }

    public void acceptWebhook(UUID workflowId,
                              Map<String, Object> body,
                              String idempotencyKey) {

        if (idempotencyKey != null &&
                incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        IncomingEvent ev = new IncomingEvent();
        ev.setWorkflowId(workflowId);
        ev.setPayload(toJson(body));
        ev.setIdempotencyKey(idempotencyKey);
        ev.setReceivedAt(OffsetDateTime.now());
        incomingEventRepository.saveAndFlush(ev);

        // 🔹 reuse existing open run if any
        Optional<WorkflowRun> existing =
                workflowRunRepository.findFirstByWorkflowIdAndStatusInOrderByStartedAtDesc(
                        workflowId,
                        List.of(WorkflowRun.Status.QUEUED, WorkflowRun.Status.RUNNING));

        WorkflowRun run = existing.orElseGet(() ->
                workflowRunService.createQueuedRun(workflowId, ev.getId())
        );

        jobPublisher.publishRun(
                run.getId(),
                workflowId,
                ev.getPayload()
        );
    }


    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
