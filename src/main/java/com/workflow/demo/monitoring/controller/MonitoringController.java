package com.workflow.demo.monitoring.controller;

import com.workflow.demo.monitoring.dto.*;
import com.workflow.demo.monitoring.service.AlertService;
import com.workflow.demo.monitoring.service.ExecutionMonitoringService;
import com.workflow.demo.monitoring.service.MonitoringService;
import com.workflow.demo.monitoring.service.ObservabilityService;
import com.workflow.demo.monitoring.service.WorkflowAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final WorkflowAnalyticsService workflowAnalyticsService;
    private final ExecutionMonitoringService executionMonitoringService;
    private final ObservabilityService observabilityService;
    private final AlertService alertService;

    public MonitoringController(MonitoringService monitoringService,
                                WorkflowAnalyticsService workflowAnalyticsService,
                                ExecutionMonitoringService executionMonitoringService,
                                ObservabilityService observabilityService,
                                AlertService alertService) {
        this.monitoringService = monitoringService;
        this.workflowAnalyticsService = workflowAnalyticsService;
        this.executionMonitoringService = executionMonitoringService;
        this.observabilityService = observabilityService;
        this.alertService = alertService;
    }

    @GetMapping("/health")
    public HealthStatusDto getHealth() {
        return monitoringService.getHealthStatus();
    }

    @GetMapping("/system")
    public SystemMetricsDto getSystemMetrics() {
        return monitoringService.getSystemMetrics();
    }

    @GetMapping("/metrics")
    public TrafficMetricsDto getMetrics() {
        return monitoringService.getTrafficMetrics();
    }

    @GetMapping("/prometheus")
    public PrometheusMetricsDto getPrometheus() {
        return monitoringService.getPrometheusMetrics();
    }

    @GetMapping("/traces")
    public TraceDto getTraces() {
        // Placeholder for traces - implement with Zipkin integration
        TraceDto dto = new TraceDto();
        dto.setWorkflow("Sample Workflow");
        dto.setDuration(1800);
        dto.setTraceId("abcd123");
        dto.setStatus("Success");
        return dto;
    }

    @GetMapping("/workers")
    public WorkerStatusDto getWorkers() {
        return monitoringService.getWorkerStatus();
    }

    // Phase 3: Workflow Analytics Endpoints
    @GetMapping("/analytics")
    public WorkflowAnalyticsDto getWorkflowAnalytics() {
        return workflowAnalyticsService.getWorkflowAnalytics();
    }

    @GetMapping("/analytics/daily-executions")
    public List<DailyExecutionDto> getDailyExecutions() {
        return workflowAnalyticsService.getDailyExecutions();
    }

    @GetMapping("/analytics/workflow-stats")
    public List<WorkflowExecutionStatsDto> getWorkflowExecutionStats() {
        return workflowAnalyticsService.getWorkflowExecutionStats();
    }

    @GetMapping("/stats")
    public WorkflowStatsDto getWorkflowStats() {
        return workflowAnalyticsService.getWorkflowStats();
    }

    // Phase 4: Execution Monitoring Endpoints
    @GetMapping("/executions/live")
    public List<LiveExecutionDto> getLiveExecutions() {
        return executionMonitoringService.getLiveExecutions();
    }

    @GetMapping("/executions/{runId}/details")
    public ExecutionDetailDto getExecutionDetails(@PathVariable UUID runId) {
        return executionMonitoringService.getExecutionDetails(runId);
    }

    // Phase 5: Advanced Observability Endpoints
    @GetMapping("/observability/links")
    public ObservabilityLinksDto getObservabilityLinks() {
        return observabilityService.getObservabilityLinks();
    }

    @GetMapping("/observability/traces")
    public List<RecentTraceDto> getRecentTraces() {
        return observabilityService.getRecentTraces();
    }

    @GetMapping("/observability/metrics-update")
    public MetricsUpdateDto getMetricsUpdateInfo() {
        return observabilityService.getMetricsUpdateInfo();
    }

    // Phase 6: Alerts and Recommendations Endpoints
    @GetMapping("/alerts")
    public List<AlertDto> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }

    @GetMapping("/failures")
    public List<FailureAnalysisDto> getFailureAnalysis() {
        return alertService.getFailureAnalysis();
    }

    @GetMapping("/recommendations")
    public List<RecommendationDto> getRecommendations() {
        return alertService.getRecommendations();
    }
}
