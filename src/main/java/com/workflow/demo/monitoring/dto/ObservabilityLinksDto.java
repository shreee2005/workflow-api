package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObservabilityLinksDto {
    private String grafanaUrl;
    private String zipkinUrl;
    private String prometheusUrl;
}
