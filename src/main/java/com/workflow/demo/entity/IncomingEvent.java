package com.workflow.demo.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;
import java.time.OffsetDateTime;
@Data
@Entity
@Table(name = "incoming_events")
public class IncomingEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt = OffsetDateTime.now();

}
