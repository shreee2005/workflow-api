package com.workflow.demo.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class WorkflowJobMessage {
    private UUID runId;
    private UUID workflowId;
    private UUID workflowVersionId;
    private String payload;
    private int attempt;

}
