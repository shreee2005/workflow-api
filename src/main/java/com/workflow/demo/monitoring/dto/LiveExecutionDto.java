package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveExecutionDto {
    private String workflow;
    private String currentStep;
    private long elapsedTime;
    private int retries;
    private String queue;
    private String worker;
    private String status;
    private UUID runId;
}
