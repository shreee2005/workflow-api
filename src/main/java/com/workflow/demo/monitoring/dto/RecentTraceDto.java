package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentTraceDto {
    private String workflow;
    private long duration;
    private String traceId;
    private String status;
    private String timestamp;
}
