package com.workflow.demo.service;

import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {

    private final WorkflowRepository workflowRepository;
    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    private final WorkflowRunRepository workflowRunRepository;

    public WebhookService(
            WorkflowRepository workflowRepository,
            IncomingEventRepository incomingEventRepository,
            JobPublisher jobPublisher,
            WorkflowRunService workflowRunService,
            WorkflowRunRepository workflowRunRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
        this.workflowRunRepository = workflowRunRepository;
    }

    public void acceptWebhook(UUID workflowId,
                              Map<String, Object> body,
                              String idempotencyKey) {

        // 1️⃣ Idempotency check
        if (idempotencyKey != null &&
                incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        // 2️⃣ Validate workflow existence
        var workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new RuntimeException("WORKFLOW_NOT_FOUND"));

        // 3️⃣ Validate workflow is active
        if (!workflow.isActive()) {
            throw new RuntimeException("WORKFLOW_NOT_ACTIVE");
        }

        // 4️⃣ Persist incoming event
        IncomingEvent ev = new IncomingEvent();
        ev.setWorkflowId(workflowId);
        ev.setPayload(toJson(body));
        ev.setIdempotencyKey(idempotencyKey);
        ev.setReceivedAt(OffsetDateTime.now());
        incomingEventRepository.saveAndFlush(ev);

        // 5️⃣ Reuse existing open run if any
        Optional<WorkflowRun> existing =
                workflowRunRepository
                        .findFirstByWorkflowIdAndStatusInOrderByStartedAtDesc(
                                workflowId,
                                List.of(
                                        WorkflowRun.Status.QUEUED,
                                        WorkflowRun.Status.RUNNING
                                )
                        );

        WorkflowRun run = existing.orElseGet(() ->
                workflowRunService.createQueuedRun(
                        workflowId,
                        ev.getId()
                )
        );

        // 6️⃣ Publish to worker
        jobPublisher.publishRun(
                run.getId(),
                workflowId,
                ev.getPayload()
        );
    }

    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}