package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficMetricsDto {
    private int requestsPerMinute;
    private double successRate;
    private double errorRate;
    private long avgLatency;
}
