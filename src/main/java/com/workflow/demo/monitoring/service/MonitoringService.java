package com.workflow.demo.monitoring.service;

import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.monitoring.dto.*;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final RabbitAdmin rabbitAdmin;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowRepository workflowRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${workflow.worker.health-url:http://workflow-worker:8080/actuator/health}")
    private String workerHealthUrl;

    public MonitoringService(MeterRegistry meterRegistry,
                           DataSource dataSource,
                           ObjectProvider<RabbitAdmin> rabbitAdminProvider,
                           WorkflowRunRepository workflowRunRepository,
                           WorkflowRepository workflowRepository) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        this.rabbitAdmin = rabbitAdminProvider.getIfAvailable();
        this.workflowRunRepository = workflowRunRepository;
        this.workflowRepository = workflowRepository;
    }

    public HealthStatusDto getHealthStatus() {
        HealthStatusDto dto = new HealthStatusDto();
        dto.setApi(checkAppHealth() ? "UP" : "DOWN");
        dto.setDatabase(checkDatabaseHealth() ? "UP" : "DOWN");
        dto.setRabbitmq(checkRabbitMQHealth() ? "UP" : "DOWN");
        dto.setRedis("UP");
        dto.setWorker(checkWorkerHealth() ? "UP" : "DOWN");
        return dto;
    }

    public SystemMetricsDto getSystemMetrics() {
        SystemMetricsDto dto = new SystemMetricsDto();

        try {
            dto.setCpu(Math.max(0, getCpuUsage()));
        } catch (Exception e) {
            dto.setCpu(0);
        }

        try {
            dto.setMemory(getMemoryUsage());
        } catch (Exception e) {
            dto.setMemory(0);
        }

        try {
            dto.setHeapUsage(Math.max(0, Math.min(100, getHeapUsage())));
        } catch (Exception e) {
            dto.setHeapUsage(0);
        }

        try {
            dto.setActiveThreads(getActiveThreads());
        } catch (Exception e) {
            dto.setActiveThreads(0);
        }

        try {
            dto.setRequestsPerMinute((int) Math.max(0, getTotalHttpRequests()));
        } catch (Exception e) {
            dto.setRequestsPerMinute(0);
        }

        try {
            dto.setAvgResponseTime((long) getAverageResponseTimeMs());
        } catch (Exception e) {
            dto.setAvgResponseTime(0);
        }

        return dto;
    }

    public TrafficMetricsDto getTrafficMetrics() {
        TrafficMetricsDto dto = new TrafficMetricsDto();

        double totalRequests = getTotalHttpRequests();
        double successRequests = getSuccessHttpRequests();
        dto.setRequestsPerMinute((int) Math.max(0, totalRequests));
        dto.setSuccessRate(totalRequests > 0 ? Math.max(0, Math.min(100, (successRequests / totalRequests) * 100)) : 100);
        dto.setErrorRate(totalRequests > 0 ? Math.max(0, Math.min(100, 100 - ((successRequests / totalRequests) * 100))) : 0);
        dto.setAvgLatency((long) getAverageResponseTimeMs());
        return dto;
    }

    public WorkerStatusDto getWorkerStatus() {
        WorkerStatusDto dto = new WorkerStatusDto();
        long running = workflowRunRepository.findAll().stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.RUNNING
                        || run.getStatus() == WorkflowRun.Status.RETRYING
                        || run.getStatus() == WorkflowRun.Status.WAITING)
                .count();
        long failed = workflowRunRepository.findAll().stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.FAILED)
                .count();

        dto.setRunningWorkers((int) running);
        dto.setHealthyWorkers(checkWorkerHealth() ? 1 : 0);
        dto.setFailedWorkers((int) failed);
        return dto;
    }

    public PrometheusMetricsDto getPrometheusMetrics() {
        PrometheusMetricsDto dto = new PrometheusMetricsDto();
        dto.setMetrics(fetchPrometheusMetrics());
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
                rabbitAdmin.getQueueInfo("workflow.tasks");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkAppHealth() {
        try {
            String response = restTemplate.getForObject("http://localhost:" + serverPort + "/actuator/health", String.class);
            return response != null && response.contains("\"status\":\"UP\"");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkWorkerHealth() {
        try {
            String response = restTemplate.getForObject(workerHealthUrl, String.class);
            return response != null && response.contains("\"status\":\"UP\"");
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchPrometheusMetrics() {
        try {
            return restTemplate.getForObject("http://localhost:" + serverPort + "/actuator/prometheus", String.class);
        } catch (Exception e) {
            return "";
        }
    }

    private double getTotalHttpRequests() {
        String metrics = fetchPrometheusMetrics();
        return extractPrometheusCounter(metrics, "http_server_requests_seconds_count");
    }

    private double getSuccessHttpRequests() {
        String metrics = fetchPrometheusMetrics();
        double success = 0;
        Matcher matcher = Pattern.compile("http_server_requests_seconds_count\\{[^\\n]*status=\\\"(200|201|202|204|206)\\\"[^\\n]*\\}\\s+([0-9eE.+-]+)")
                .matcher(metrics);
        while (matcher.find()) {
            success += Double.parseDouble(matcher.group(2));
        }
        return success;
    }

    private double getAverageResponseTimeMs() {
        String metrics = fetchPrometheusMetrics();
        double totalSeconds = extractPrometheusCounter(metrics, "http_server_requests_seconds_sum");
        double totalRequests = extractPrometheusCounter(metrics, "http_server_requests_seconds_count");
        return totalRequests > 0 ? (totalSeconds / totalRequests) * 1000 : 0;
    }

    private double extractPrometheusCounter(String metrics, String metricName) {
        if (metrics == null || metrics.isBlank()) {
            return 0;
        }
        double total = 0;
        Matcher matcher = Pattern.compile(Pattern.quote(metricName) + "\\{[^\\n]*\\}\\s+([0-9eE.+-]+)")
                .matcher(metrics);
        while (matcher.find()) {
            total += Double.parseDouble(matcher.group(1));
        }

        Matcher scalarMatcher = Pattern.compile(Pattern.quote(metricName) + "\\s+([0-9eE.+-]+)")
                .matcher(metrics);
        while (scalarMatcher.find()) {
            total += Double.parseDouble(scalarMatcher.group(1));
        }

        return total;
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
        return usedMemory / (1024 * 1024);
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
