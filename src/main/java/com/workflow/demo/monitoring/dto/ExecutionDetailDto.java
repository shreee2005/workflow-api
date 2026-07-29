package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDetailDto {
    private UUID runId;
    private String workflowName;
    private String status;
    private long totalDuration;
    private int currentStepIndex;
    private List<ExecutionTimelineDto> timeline;
}
