package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAnalyticsDto {
    private int totalWorkflows;
    private String mostUsedWorkflow;
    private String mostActiveTeam;
    private double averageRuntime;
    private String longestWorkflow;
    private double successRate;
}
