package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatsDto {
    private int running;
    private int queued;
    private int completedToday;
    private int failed;
}
