package com.workflow.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "incoming_event_id")
    private UUID incomingEventId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.QUEUED;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    public enum Status {
        CREATED,
        QUEUED,
        WAITING,
        RUNNING,
        RETRYING,
        SUCCEEDED,
        FAILED
    }


    @Column(nullable = false)
    private int attempt = 0;

    @Column(nullable = false)
    private int maxAttempts = 3;

    @Column(name = "dead_lettered", nullable = false)
    private boolean deadLettered = false;

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
    }

    public void setWorkflowVersionId(UUID workflowVersionId) {
        this.workflowVersionId = workflowVersionId;
    }

    public void transitionTo(Status next) {
        if (!isValidTransition(this.status, next)) {
            throw new IllegalStateException(
                    "Invalid state transition: " + this.status + " → " + next
            );
        }
        this.status = next;
    }

    private boolean isValidTransition(Status from, Status to) {
        return switch (from) {
            case WAITING -> to == Status.RUNNING;
            case CREATED -> to == Status.RUNNING;
            case RETRYING -> to == Status.RETRYING;
            case QUEUED -> to == Status.RUNNING;
            case RUNNING -> to == Status.WAITING || to == Status.SUCCEEDED || to == Status.FAILED;
            case FAILED, SUCCEEDED -> false;
        };
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public UUID getIncomingEventId() {
        return incomingEventId;
    }

    public void setIncomingEventId(UUID incomingEventId) {
        this.incomingEventId = incomingEventId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isDeadLettered() {
        return deadLettered;
    }

    public void setDeadLettered(boolean deadLettered) {
        this.deadLettered = deadLettered;
    }


}
