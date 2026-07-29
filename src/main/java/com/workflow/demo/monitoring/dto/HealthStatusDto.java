package com.workflow.demo.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {
    private String api;
    private String database;
    private String redis;
    private String rabbitmq;
    private String worker;
}
