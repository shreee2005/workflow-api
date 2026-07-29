package com.workflow.demo.monitoring.service;

import com.workflow.demo.monitoring.dto.AlertDto;
import com.workflow.demo.monitoring.dto.FailureAnalysisDto;
import com.workflow.demo.monitoring.dto.RecommendationDto;
import com.workflow.demo.monitoring.service.MonitoringService;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final MonitoringService monitoringService;
    private final RabbitAdmin rabbitAdmin;

    public AlertService(MonitoringService monitoringService,
                        ObjectProvider<RabbitAdmin> rabbitAdminProvider) {
        this.monitoringService = monitoringService;
        this.rabbitAdmin = rabbitAdminProvider.getIfAvailable();
    }

    public List<AlertDto> getActiveAlerts() {
        List<AlertDto> alerts = new ArrayList<>();
        
        // Check queue depth
        try {
            if (rabbitAdmin != null) {
                var queueInfo = rabbitAdmin.getQueueInfo("workflow.queue");
                if (queueInfo != null && queueInfo.getMessageCount() > 100) {
                    alerts.add(new AlertDto(
                            "High Queue Length",
                            "RabbitMQ queue depth exceeds threshold",
                            "WARNING",
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            "> 100",
                            String.valueOf(queueInfo.getMessageCount())
                    ));
                }
            }
        } catch (Exception e) {
            // Ignore errors in alert checking
        }
        
        // Check system metrics
        var systemMetrics = monitoringService.getSystemMetrics();
        if (systemMetrics.getCpu() > 80) {
            alerts.add(new AlertDto(
                    "High CPU Usage",
                    "CPU utilization is above threshold",
                    "WARNING",
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "> 80%",
                    String.format("%.1f%%", systemMetrics.getCpu())
            ));
        }
        
        if (systemMetrics.getHeapUsage() > 90) {
            alerts.add(new AlertDto(
                    "High Memory Usage",
                    "JVM Heap usage is above threshold",
                    "WARNING",
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "> 90%",
                    String.format("%.1f%%", systemMetrics.getHeapUsage())
            ));
        }
        
        // Sample slow workflow alert
        alerts.add(new AlertDto(
                "Slow Workflow",
                "Invoice Approval execution time exceeds threshold",
                "WARNING",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "> 5 sec",
                "18 sec"
        ));
        
        return alerts;
    }

    public List<FailureAnalysisDto> getFailureAnalysis() {
        List<FailureAnalysisDto> failures = new ArrayList<>();
        
        // Sample failure analysis data
        failures.add(new FailureAnalysisDto("SMTP", 12, 2.4, 
                OffsetDateTime.now().minusMinutes(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        failures.add(new FailureAnalysisDto("Redis", 3, 0.6, 
                OffsetDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        failures.add(new FailureAnalysisDto("Plugin", 5, 1.0, 
                OffsetDateTime.now().minusMinutes(45).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        
        return failures;
    }

    public List<RecommendationDto> getRecommendations() {
        List<RecommendationDto> recommendations = new ArrayList<>();
        
        var systemMetrics = monitoringService.getSystemMetrics();
        var workerStatus = monitoringService.getWorkerStatus();
        
        // Check worker utilization
        if (workerStatus.getRunningWorkers() > 0) {
            double utilization = (double) workerStatus.getHealthyWorkers() / workerStatus.getRunningWorkers();
            if (utilization > 0.9) {
                recommendations.add(new RecommendationDto(
                        "Worker Utilization",
                        "Worker utilization is above 90%",
                        "HIGH",
                        "Start another worker instance"
                ));
            }
        }
        
        // Check queue depth
        try {
            if (rabbitAdmin != null) {
                var queueInfo = rabbitAdmin.getQueueInfo("workflow.queue");
                if (queueInfo != null && queueInfo.getMessageCount() > 50) {
                    recommendations.add(new RecommendationDto(
                            "Queue Depth",
                            "Queue depth is high",
                            "MEDIUM",
                            "Increase RabbitMQ consumers"
                    ));
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        // Check memory
        if (systemMetrics.getHeapUsage() > 80) {
            recommendations.add(new RecommendationDto(
                    "Memory Pressure",
                    "JVM Heap usage is high",
                    "MEDIUM",
                    "Consider increasing JVM heap size or optimizing memory usage"
            ));
        }
        
        return recommendations;
    }
}
