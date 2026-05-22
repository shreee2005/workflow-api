package com.workflow.demo.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class WorkflowTemplateDto {
    private UUID id;
    private String name;
    private String category;
    private String description;
    private String spec;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
