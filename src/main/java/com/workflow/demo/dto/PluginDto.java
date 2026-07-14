package com.workflow.demo.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PluginDto {
    private UUID id;
    private String key;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String configSchema;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
