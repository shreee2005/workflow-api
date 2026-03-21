// Workflow.java
package com.workflow.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflows")
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)  // <-- ADD THIS LINE
    private String spec;

    @Column(name = "active_version_id")
    private UUID activeVersionId;

    private boolean active = false;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
