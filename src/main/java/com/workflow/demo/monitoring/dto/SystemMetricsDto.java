package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsDto {
    private double cpu;
    private long memory;
    private int requestsPerMinute;
    private long avgResponseTime;
    private int activeThreads;
    private double heapUsage;
}
