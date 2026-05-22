package com.workflow.demo.dto;

import lombok.Data;

@Data
public class CreateWorkflowFromTemplateRequest {
    private String workflowName;
    private boolean active;
    private String changeNote;
}
