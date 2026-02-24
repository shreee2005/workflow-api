package com.workflow.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

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
        QUEUED,
        RUNNING,
        SUCCEEDED,
        FAILED
    }

    @Column(nullable = false)
    private int attempt = 0;

    @Column(nullable = false)
    private int maxAttempts = 3;

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
            case QUEUED -> to == Status.RUNNING;
            case RUNNING -> to == Status.SUCCEEDED || to == Status.FAILED;
            case FAILED, SUCCEEDED -> false;
        };
    }

}
