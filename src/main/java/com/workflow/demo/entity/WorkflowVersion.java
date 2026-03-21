package com.workflow.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "workflow_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_workflow_version_number", columnNames = {"workflow_id", "version_number"})
        }
)
public class WorkflowVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "spec", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String spec;

    @Column(name = "change_note")
    private String changeNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}