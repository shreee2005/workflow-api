package com.workflow.demo.service;
import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
<<<<<<< HEAD
import com.workflow.demo.observability.WorkflowMetricsService;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
=======
import com.workflow.demo.entity.WorkflowVersion;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import com.workflow.demo.repository.WorkflowVersionRepository;
>>>>>>> 7379d8e (Non-retry and retry)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {
<<<<<<< HEAD

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

=======
    private final WorkflowVersionRepository workflowVersionRepository;
>>>>>>> 7379d8e (Non-retry and retry)
    private final WorkflowRepository workflowRepository;
    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    private final WorkflowMetricsService workflowMetricsService;

    @Value("${app.queue.workflow-runs:workflow.tasks}")
    private String workflowRunsQueueName;

    public WebhookService(
            WorkflowVersionRepository workflowVersionRepository,
            WorkflowRepository workflowRepository,
            IncomingEventRepository incomingEventRepository,
            JobPublisher jobPublisher,
            WorkflowRunService workflowRunService,
            WorkflowMetricsService workflowMetricsService
    ) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowRepository = workflowRepository;
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
        this.workflowMetricsService = workflowMetricsService;
    }

    @Transactional
    public void acceptWebhook(
            UUID workflowId,
            Map<String, Object> body,
            String idempotencyKey
    ) {
        Timer.Sample sample = workflowMetricsService.startStepTimer();

<<<<<<< HEAD
        try {
            log.info("WEBHOOK_HIT workflowId={} idemKey={}", workflowId, idempotencyKey);

            // 1) idempotency
            if (idempotencyKey != null && incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("WEBHOOK_DUPLICATE_SKIPPED idemKey={}", idempotencyKey);
                workflowMetricsService.stopStepTimer(sample, "webhook_accept", "success");
                return;
            }

            // 2) validate workflow
            var workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("WORKFLOW_NOT_FOUND"));

            if (!workflow.isActive()) {
                throw new RuntimeException("WORKFLOW_NOT_ACTIVE");
            }

            var workflowVersionId = workflow.getActiveVersionId();
            if (workflowVersionId == null) {
                throw new RuntimeException("WORKFLOW_VERSION_NOT_FOUND");
            }

            // 3) save incoming event
            IncomingEvent ev = new IncomingEvent();
            ev.setWorkflowId(workflowId);
            ev.setPayload(toJson(body));
            ev.setIdempotencyKey(idempotencyKey);
            ev.setReceivedAt(OffsetDateTime.now());
            ev = incomingEventRepository.saveAndFlush(ev);
            log.info("INCOMING_EVENT_SAVED id={}", ev.getId());

            // 4) ALWAYS create a fresh run (debug-safe, deterministic)
            WorkflowRun run = workflowRunService.createQueuedRun(workflowId, workflowVersionId, ev.getId());
            log.info("RUN_SAVED id={} workflowId={} versionId={}", run.getId(), workflowId, workflowVersionId);

            // 5) publish
            log.info("PUBLISHING_RUN id={} queue={}", run.getId(), workflowRunsQueueName);
            jobPublisher.publishRun(run.getId(), workflowId, workflowVersionId, ev.getPayload());

            // 6) metrics
            workflowMetricsService.incrementWorkflowRuns();
            workflowMetricsService.refreshQueueBacklog(workflowRunsQueueName);
            workflowMetricsService.stopStepTimer(sample, "webhook_accept", "success");

        } catch (Exception ex) {
            workflowMetricsService.incrementWorkflowFailures();
            workflowMetricsService.stopStepTimer(sample, "webhook_accept", "failed");
            log.error("Failed to accept webhook for workflowId={}", workflowId, ex);
            throw ex;
        }
=======
        // 1) Idempotency
        if (idempotencyKey != null &&
                incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        // 2) Load workflow
        var workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("WORKFLOW_NOT_FOUND"));

        // 3) Resolve version
        UUID versionId = workflow.getActiveVersionId();
        if (versionId == null) {
            versionId = workflowVersionRepository
                    .findTopByWorkflowIdOrderByVersionNumberDesc(workflowId)
                    .map(WorkflowVersion::getId)
                    .orElseThrow(() -> new RuntimeException("WORKFLOW_VERSION_NOT_FOUND"));
        }

        // 4) Must be active
        if (!workflow.isActive()) {
            throw new RuntimeException("WORKFLOW_NOT_ACTIVE");
        }

        // 5) Persist incoming event
        IncomingEvent ev = new IncomingEvent();
        ev.setWorkflowId(workflowId);
        ev.setPayload(toJson(body));
        ev.setIdempotencyKey(idempotencyKey);
        ev.setReceivedAt(OffsetDateTime.now());
        incomingEventRepository.saveAndFlush(ev);

        final UUID resolvedVersionId = versionId;
        // 6) Reuse open run if any, else create
        Optional<WorkflowRun> existing =
                workflowRunRepository.findFirstByWorkflowIdAndStatusInOrderByStartedAtDesc(
                        workflowId,
                        List.of(WorkflowRun.Status.QUEUED, WorkflowRun.Status.RUNNING)
                );

        WorkflowRun run = existing.orElseGet(() ->
                workflowRunService.createQueuedRun(workflowId, ev.getId(), resolvedVersionId)
        );

        // 7) Publish to worker with versionId
        jobPublisher.publishRun(
                run.getId(),
                workflowId,
                run.getWorkflowVersionId() != null ? run.getWorkflowVersionId() : resolvedVersionId,
                ev.getPayload()
        );
>>>>>>> 7379d8e (Non-retry and retry)
    }

    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}