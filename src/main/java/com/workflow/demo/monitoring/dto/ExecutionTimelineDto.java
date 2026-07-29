package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTimelineDto {
    private String step;
    private String status;
    private OffsetDateTime timestamp;
    private String details;
}
