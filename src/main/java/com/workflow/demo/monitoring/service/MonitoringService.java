package com.workflow.demo.monitoring.service;

import com.workflow.demo.monitoring.dto.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;
    private final DataSource dataSource;
    private final RabbitAdmin rabbitAdmin;

    public MonitoringService(MeterRegistry meterRegistry,
                           HealthEndpoint healthEndpoint,
                           DataSource dataSource,
                           ObjectProvider<RabbitAdmin> rabbitAdminProvider) {
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
        this.dataSource = dataSource;
        this.rabbitAdmin = rabbitAdminProvider.getIfAvailable();
    }

    public HealthStatusDto getHealthStatus() {
        HealthStatusDto dto = new HealthStatusDto();
        
        // API Health
        dto.setApi("UP");
        
        // Database Health
        dto.setDatabase(checkDatabaseHealth() ? "UP" : "DOWN");
        
        // RabbitMQ Health
        dto.setRabbitmq(checkRabbitMQHealth() ? "UP" : "DOWN");
        
        // Redis Health (placeholder - implement if Redis is used)
        dto.setRedis("UP");
        
        // Worker Health (placeholder - implement worker health check)
        dto.setWorker("UP");
        
        return dto;
    }

    public SystemMetricsDto getSystemMetrics() {
        SystemMetricsDto dto = new SystemMetricsDto();
        
        // CPU Usage
        try {
            double cpuUsage = getCpuUsage();
            dto.setCpu(Math.max(0, cpuUsage));
        } catch (Exception e) {
            dto.setCpu(0);
        }
        
        // Memory Usage in MB
        try {
            long memoryUsage = getMemoryUsage();
            dto.setMemory(memoryUsage);
        } catch (Exception e) {
            dto.setMemory(0);
        }
        
        // JVM Heap Usage percentage
        try {
            double heapUsage = getHeapUsage();
            dto.setHeapUsage(Math.max(0, Math.min(100, heapUsage)));
        } catch (Exception e) {
            dto.setHeapUsage(0);
        }
        
        // Active Threads
        try {
            int activeThreads = getActiveThreads();
            dto.setActiveThreads(activeThreads);
        } catch (Exception e) {
            dto.setActiveThreads(0);
        }
        
        // Request Count per minute (from Micrometer)
        try {
            double requestsPerMinute = meterRegistry.counter("http.server.requests").count();
            dto.setRequestsPerMinute((int) requestsPerMinute);
        } catch (Exception e) {
            dto.setRequestsPerMinute(0);
        }
        
        // Average Response Time
        try {
            double avgResponseTime = meterRegistry.timer("http.server.requests").mean(TimeUnit.MILLISECONDS);
            dto.setAvgResponseTime((long) avgResponseTime);
        } catch (Exception e) {
            dto.setAvgResponseTime(0);
        }
        
        return dto;
    }

    public TrafficMetricsDto getTrafficMetrics() {
        TrafficMetricsDto dto = new TrafficMetricsDto();
        
        // Requests per minute
        try {
            double requestsPerMinute = meterRegistry.counter("http.server.requests").count();
            dto.setRequestsPerMinute((int) requestsPerMinute);
        } catch (Exception e) {
            dto.setRequestsPerMinute(0);
        }
        
        // Success Rate (calculate from HTTP status metrics)
        try {
            double successCount = meterRegistry.counter("http.server.requests", "status", "200").count() +
                                 meterRegistry.counter("http.server.requests", "status", "201").count();
            double totalCount = meterRegistry.counter("http.server.requests").count();
            double successRate = totalCount > 0 ? (successCount / totalCount) * 100 : 100;
            dto.setSuccessRate(Math.max(0, Math.min(100, successRate)));
            dto.setErrorRate(Math.max(0, Math.min(100, 100 - successRate)));
        } catch (Exception e) {
            dto.setSuccessRate(100);
            dto.setErrorRate(0);
        }
        
        // Average Latency
        try {
            double avgLatency = meterRegistry.timer("http.server.requests").mean(TimeUnit.MILLISECONDS);
            dto.setAvgLatency((long) avgLatency);
        } catch (Exception e) {
            dto.setAvgLatency(0);
        }
        
        return dto;
    }

    public WorkerStatusDto getWorkerStatus() {
        WorkerStatusDto dto = new WorkerStatusDto();
        
        // Placeholder values - implement actual worker monitoring
        dto.setRunningWorkers(3);
        dto.setHealthyWorkers(3);
        dto.setFailedWorkers(0);
        
        return dto;
    }

    public PrometheusMetricsDto getPrometheusMetrics() {
        // This would typically proxy to the actual Prometheus endpoint
        // For now, return a placeholder
        PrometheusMetricsDto dto = new PrometheusMetricsDto();
        dto.setMetrics("# Prometheus metrics would be scraped from /actuator/prometheus");
        return dto;
    }

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRabbitMQHealth() {
        try {
            if (rabbitAdmin != null) {
                rabbitAdmin.getQueueInfo("workflow.queue");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemCpuLoad();
            return cpuLoad >= 0 ? cpuLoad * 100 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long getMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        return usedMemory / (1024 * 1024); // Convert to MB
    }

    private double getHeapUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return max > 0 ? ((double) used / max) * 100 : 0;
    }

    private int getActiveThreads() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.getThreadCount();
    }
}
