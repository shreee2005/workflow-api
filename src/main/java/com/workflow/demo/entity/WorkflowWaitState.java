package com.workflow.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_wait_states")
public class WorkflowWaitState {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, unique = true)
    private UUID runId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 200)
    private String correlationId;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // WAITING / RESUMED / TIMED_OUT

    @Column(name = "callback_payload", columnDefinition = "text")
    private String callbackPayload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resumed_at")
    private OffsetDateTime resumedAt;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }

    public UUID getWorkflowId() { return workflowId; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }

    public UUID getWorkflowVersionId() { return workflowVersionId; }
    public void setWorkflowVersionId(UUID workflowVersionId) { this.workflowVersionId = workflowVersionId; }

    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCallbackPayload() { return callbackPayload; }
    public void setCallbackPayload(String callbackPayload) { this.callbackPayload = callbackPayload; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getResumedAt() { return resumedAt; }
    public void setResumedAt(OffsetDateTime resumedAt) { this.resumedAt = resumedAt; }
}