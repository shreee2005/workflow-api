package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailureAnalysisDto {
    private String component;
    private int failureCount;
    private double failureRate;
    private String lastFailure;
}
