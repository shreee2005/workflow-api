package com.workflow.demo.service;

import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.observability.WorkflowMetricsService;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WorkflowRepository workflowRepository;
    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowMetricsService workflowMetricsService;

    @Value("${app.queue.workflow-runs:workflow.runs}")
    private String workflowRunsQueueName;

    public WebhookService(
            WorkflowRepository workflowRepository,
            IncomingEventRepository incomingEventRepository,
            JobPublisher jobPublisher,
            WorkflowRunService workflowRunService,
            WorkflowRunRepository workflowRunRepository,
            WorkflowMetricsService workflowMetricsService
    ) {
        this.workflowRepository = workflowRepository;
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
        this.workflowRunRepository = workflowRunRepository;
        this.workflowMetricsService = workflowMetricsService;
    }

    public void acceptWebhook(UUID workflowId,
                              Map<String, Object> body,
                              String idempotencyKey) {

        Timer.Sample sample = workflowMetricsService.startStepTimer();

        try {
            // 1️⃣ Idempotency check first (don't count duplicate retries as runs)
            if (idempotencyKey != null &&
                    incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
                workflowMetricsService.stopStepTimer(sample, "webhook_accept", "success");
                return;
            }

            // 2️⃣ Count only accepted new processing attempt
            workflowMetricsService.incrementWorkflowRuns();

            // 3️⃣ Validate workflow existence
            var workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("WORKFLOW_NOT_FOUND"));

            // 4️⃣ Validate workflow is active
            if (!workflow.isActive()) {
                throw new RuntimeException("WORKFLOW_NOT_ACTIVE");
            }

            // 5️⃣ Persist incoming event
            IncomingEvent ev = new IncomingEvent();
            ev.setWorkflowId(workflowId);
            ev.setPayload(toJson(body));
            ev.setIdempotencyKey(idempotencyKey);
            ev.setReceivedAt(OffsetDateTime.now());
            incomingEventRepository.saveAndFlush(ev);

            // 6️⃣ Reuse existing open run if any
            Optional<WorkflowRun> existing =
                    workflowRunRepository.findFirstByWorkflowIdAndStatusInOrderByStartedAtDesc(
                            workflowId,
                            List.of(WorkflowRun.Status.QUEUED, WorkflowRun.Status.RUNNING)
                    );

            var workflowVersionId = workflow.getActiveVersionId();
            if (workflowVersionId == null) {
                throw new RuntimeException("WORKFLOW_VERSION_NOT_FOUND");
            }

            WorkflowRun run = existing.orElseGet(() ->
                    workflowRunService.createQueuedRun(workflowId, workflowVersionId, ev.getId())
            );

            // 7️⃣ Publish to worker
            jobPublisher.publishRun(run.getId(), workflowId, workflowVersionId , ev.getPayload());

            // 8️⃣ Refresh queue backlog gauge
            workflowMetricsService.refreshQueueBacklog(workflowRunsQueueName);

            workflowMetricsService.stopStepTimer(sample, "webhook_accept", "success");
        } catch (Exception ex) {
            workflowMetricsService.incrementWorkflowFailures();
            workflowMetricsService.stopStepTimer(sample, "webhook_accept", "failed");
            log.error("Failed to accept webhook for workflowId={}", workflowId, ex);
            throw ex;
        }
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