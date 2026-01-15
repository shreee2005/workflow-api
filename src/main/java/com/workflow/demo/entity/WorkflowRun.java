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
}
