package com.workflow.demo.monitoring.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.monitoring.dto.*;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WorkflowAnalyticsService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowAnalyticsService(WorkflowRepository workflowRepository,
                                    WorkflowRunRepository workflowRunRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowRunRepository = workflowRunRepository;
    }

    public WorkflowAnalyticsDto getWorkflowAnalytics() {
        WorkflowAnalyticsDto dto = new WorkflowAnalyticsDto();

        dto.setTotalWorkflows((int) workflowRepository.count());
        dto.setMostUsedWorkflow(findMostUsedWorkflow());
        dto.setMostActiveTeam("N/A");
        dto.setAverageRuntime(calculateAverageRuntime());
        dto.setLongestWorkflow(findLongestWorkflow());
        dto.setSuccessRate(calculateSuccessRate());
        return dto;
    }

    public List<DailyExecutionDto> getDailyExecutions() {
        List<DailyExecutionDto> dailyExecutions = new ArrayList<>();
        Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            countsByDate.put(date, 0L);
        }

        for (WorkflowRun run : workflowRunRepository.findAll()) {
            if (run.getStartedAt() == null) {
                continue;
            }
            LocalDate startedDate = run.getStartedAt().toLocalDate();
            if (countsByDate.containsKey(startedDate)) {
                countsByDate.put(startedDate, countsByDate.get(startedDate) + 1L);
            }
        }

        for (Map.Entry<LocalDate, Long> entry : countsByDate.entrySet()) {
            String dayName = entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dailyExecutions.add(new DailyExecutionDto(dayName, entry.getValue().intValue()));
        }

        return dailyExecutions;
    }

    public List<WorkflowExecutionStatsDto> getWorkflowExecutionStats() {
        List<WorkflowExecutionStatsDto> stats = new ArrayList<>();
        List<Workflow> workflows = workflowRepository.findAll();

        for (Workflow workflow : workflows) {
            List<WorkflowRun> runs = workflowRunRepository.findAll().stream()
                    .filter(run -> run.getWorkflowId().equals(workflow.getId()))
                    .toList();
            long total = runs.size();
            long successCount = runs.stream()
                    .filter(run -> run.getStatus() == WorkflowRun.Status.SUCCEEDED)
                    .count();
            double successRate = total > 0 ? (successCount * 100.0) / total : 0.0;
            stats.add(new WorkflowExecutionStatsDto(workflow.getName(), (int) total, successRate));
        }

        return stats;
    }

    public WorkflowStatsDto getWorkflowStats() {
        WorkflowStatsDto dto = new WorkflowStatsDto();
        List<WorkflowRun> runs = workflowRunRepository.findAll();

        dto.setRunning((int) runs.stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.RUNNING || run.getStatus() == WorkflowRun.Status.RETRYING || run.getStatus() == WorkflowRun.Status.WAITING)
                .count());
        dto.setQueued((int) runs.stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.QUEUED)
                .count());
        OffsetDateTime todayStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        dto.setCompletedToday((int) runs.stream()
                .filter(run -> run.getStartedAt() != null && !run.getStartedAt().isBefore(todayStart))
                .count());
        dto.setFailed((int) runs.stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.FAILED)
                .count());
        return dto;
    }

    private String findMostUsedWorkflow() {
        Map<String, Long> countsByWorkflow = new LinkedHashMap<>();
        for (WorkflowRun run : workflowRunRepository.findAll()) {
            Workflow workflow = workflowRepository.findById(run.getWorkflowId()).orElse(null);
            if (workflow == null || workflow.getName() == null) {
                continue;
            }
            countsByWorkflow.put(workflow.getName(), countsByWorkflow.getOrDefault(workflow.getName(), 0L) + 1L);
        }
        return countsByWorkflow.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No workflows");
    }

    private double calculateAverageRuntime() {
        List<WorkflowRun> runs = workflowRunRepository.findAll();
        if (runs.isEmpty()) {
            return 0.0;
        }

        long totalDurationSeconds = 0;
        int count = 0;
        for (WorkflowRun run : runs) {
            if (run.getStartedAt() != null && run.getFinishedAt() != null) {
                totalDurationSeconds += run.getFinishedAt().toEpochSecond() - run.getStartedAt().toEpochSecond();
                count++;
            }
        }

        return count > 0 ? (double) totalDurationSeconds / count : 0.0;
    }

    private String findLongestWorkflow() {
        String longestWorkflow = "No workflow data";
        double longestRuntime = -1;

        for (Workflow workflow : workflowRepository.findAll()) {
            List<WorkflowRun> runs = workflowRunRepository.findAll().stream()
                    .filter(run -> run.getWorkflowId().equals(workflow.getId()))
                    .toList();
            double averageRuntime = runs.stream()
                    .filter(run -> run.getStartedAt() != null && run.getFinishedAt() != null)
                    .mapToLong(run -> run.getFinishedAt().toEpochSecond() - run.getStartedAt().toEpochSecond())
                    .average()
                    .orElse(0.0);
            if (averageRuntime > longestRuntime) {
                longestRuntime = averageRuntime;
                longestWorkflow = workflow.getName();
            }
        }

        return longestWorkflow;
    }

    private double calculateSuccessRate() {
        List<WorkflowRun> runs = workflowRunRepository.findAll();
        if (runs.isEmpty()) {
            return 100.0;
        }

        long successCount = runs.stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.SUCCEEDED)
                .count();

        return ((double) successCount / runs.size()) * 100;
    }
}
