package com.workflow.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.demo.entity.IncomingEvent;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.observability.WorkflowMetricsService;
import com.workflow.demo.repository.IncomingEventRepository;
import com.workflow.demo.repository.WorkflowRepository;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkflowRepository workflowRepository;
    private final IncomingEventRepository incomingEventRepository;
    private final JobPublisher jobPublisher;
    private final WorkflowRunService workflowRunService;
    private final WorkflowMetricsService workflowMetricsService;

    @Value("${app.queue.workflow-runs:workflow.tasks}")
    private String workflowRunsQueueName;

    public WebhookService(
            WorkflowRepository workflowRepository,
            IncomingEventRepository incomingEventRepository,
            JobPublisher jobPublisher,
            WorkflowRunService workflowRunService,
            WorkflowMetricsService workflowMetricsService
    ) {
        this.workflowRepository = workflowRepository;
        this.incomingEventRepository = incomingEventRepository;
        this.jobPublisher = jobPublisher;
        this.workflowRunService = workflowRunService;
        this.workflowMetricsService = workflowMetricsService;
    }

    @Transactional
    @WithSpan("webhook.accept")
    public void acceptWebhook(
            @SpanAttribute("workflow.id") UUID workflowId,
            Map<String, Object> body,
            @SpanAttribute("idempotency.key") String idempotencyKey
    ) {
        Timer.Sample sample = workflowMetricsService.startStepTimer();

        try {
            log.info("WEBHOOK_HIT workflowId={} idemKey={}", workflowId, idempotencyKey);

            Span currentSpan = Span.current();

            if (idempotencyKey != null && incomingEventRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("WEBHOOK_DUPLICATE_SKIPPED idemKey={}", idempotencyKey);
                currentSpan.setAttribute("webhook.duplicate", true);
                workflowMetricsService.stopStepTimer(sample, "webhook_accept", "success");
                return;
            }

            var workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("WORKFLOW_NOT_FOUND"));

            if (!workflow.isActive()) {
                throw new RuntimeException("WORKFLOW_NOT_ACTIVE");
            }

            UUID workflowVersionId = workflow.getActiveVersionId();
            if (workflowVersionId == null) {
                throw new RuntimeException("WORKFLOW_VERSION_NOT_FOUND");
            }

            IncomingEvent ev = new IncomingEvent();
            ev.setWorkflowId(workflowId);
            ev.setPayload(toJson(body));
            ev.setIdempotencyKey(idempotencyKey);
            ev.setReceivedAt(OffsetDateTime.now());
            ev = incomingEventRepository.saveAndFlush(ev);

            WorkflowRun run = workflowRunService.createQueuedRun(workflowId, workflowVersionId, ev.getId());

            currentSpan.setAttribute("run.id", run.getId().toString());
            currentSpan.setAttribute("workflow.version.id", workflowVersionId.toString());

            jobPublisher.publishRun(run.getId(), workflowId, workflowVersionId, ev.getPayload());

            workflowMetricsService.incrementWorkflowRuns();
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
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
