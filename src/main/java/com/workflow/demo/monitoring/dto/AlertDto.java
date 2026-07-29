package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private String type;
    private String message;
    private String severity;
    private String timestamp;
    private String threshold;
    private String currentValue;
}
