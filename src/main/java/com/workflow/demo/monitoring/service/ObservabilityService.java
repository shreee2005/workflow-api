package com.workflow.demo.monitoring.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.monitoring.dto.MetricsUpdateDto;
import com.workflow.demo.monitoring.dto.ObservabilityLinksDto;
import com.workflow.demo.monitoring.dto.RecentTraceDto;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ObservabilityService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;

    @Value("${workflow.observability.grafana-url:http://localhost:3000}")
    private String grafanaUrl;

    @Value("${workflow.observability.zipkin-url:http://localhost:9411}")
    private String zipkinUrl;

    @Value("${workflow.observability.prometheus-url:http://localhost:9090}")
    private String prometheusUrl;

    public ObservabilityService(WorkflowRepository workflowRepository,
                               WorkflowRunRepository workflowRunRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowRunRepository = workflowRunRepository;
    }

    public ObservabilityLinksDto getObservabilityLinks() {
        ObservabilityLinksDto dto = new ObservabilityLinksDto();
        dto.setGrafanaUrl(grafanaUrl);
        dto.setZipkinUrl(zipkinUrl);
        dto.setPrometheusUrl(prometheusUrl);
        return dto;
    }

    public List<RecentTraceDto> getRecentTraces() {
        List<RecentTraceDto> traces = new ArrayList<>();
        List<WorkflowRun> runs = workflowRunRepository.findAll().stream()
                .sorted(Comparator.comparing(WorkflowRun::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();

        for (WorkflowRun run : runs) {
            Workflow workflow = workflowRepository.findById(run.getWorkflowId()).orElse(null);
            String workflowName = workflow != null ? workflow.getName() : "Unknown Workflow";
            long duration = 0;
            if (run.getStartedAt() != null && run.getFinishedAt() != null) {
                duration = run.getFinishedAt().toEpochSecond() - run.getStartedAt().toEpochSecond();
            } else if (run.getStartedAt() != null) {
                duration = OffsetDateTime.now().toEpochSecond() - run.getStartedAt().toEpochSecond();
            }
            traces.add(new RecentTraceDto(
                    workflowName,
                    duration,
                    run.getId() != null ? run.getId().toString() : "unassigned",
                    run.getStatus().name(),
                    run.getStartedAt() != null ? run.getStartedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            ));
        }

        return traces;
    }

    public MetricsUpdateDto getMetricsUpdateInfo() {
        MetricsUpdateDto dto = new MetricsUpdateDto();
        OffsetDateTime lastRunTime = workflowRunRepository.findAll().stream()
                .map(WorkflowRun::getStartedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.now());
        dto.setLastUpdated(lastRunTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        dto.setSecondsAgo((int) Math.max(0, OffsetDateTime.now().toEpochSecond() - lastRunTime.toEpochSecond()));
        return dto;
    }
}
