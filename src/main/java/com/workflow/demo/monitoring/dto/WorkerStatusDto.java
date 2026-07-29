package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerStatusDto {
    private int runningWorkers;
    private int healthyWorkers;
    private int failedWorkers;
}
