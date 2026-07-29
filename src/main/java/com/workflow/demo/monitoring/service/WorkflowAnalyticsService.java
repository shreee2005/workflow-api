package com.workflow.demo.monitoring.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.monitoring.dto.*;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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
        
        // Total Workflows
        int totalWorkflows = (int) workflowRepository.count();
        dto.setTotalWorkflows(totalWorkflows);
        
        // Most Used Workflow
        dto.setMostUsedWorkflow(findMostUsedWorkflow());
        
        // Most Active Team (placeholder - implement team analytics)
        dto.setMostActiveTeam("Development");
        
        // Average Runtime
        dto.setAverageRuntime(calculateAverageRuntime());
        
        // Longest Workflow
        dto.setLongestWorkflow(findLongestWorkflow());
        
        // Success Rate
        dto.setSuccessRate(calculateSuccessRate());
        
        return dto;
    }

    public List<DailyExecutionDto> getDailyExecutions() {
        List<DailyExecutionDto> dailyExecutions = new ArrayList<>();
        
        // Get last 7 days of executions
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Long> dailyCounts = new LinkedHashMap<>();
        
        for (int i = 6; i >= 0; i--) {
            OffsetDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime dayEnd = dayStart.plusDays(1);
            
            String dayName = dayStart.getDayOfWeek().toString().substring(0, 3);
            long count = workflowRunRepository.count();
            
            // In a real implementation, you'd add a custom query to filter by date range
            // For now, we'll use placeholder data
            dailyCounts.put(dayName, count);
        }
        
        // Convert to DTOs with some sample data for demonstration
        dailyExecutions.add(new DailyExecutionDto("Mon", 210));
        dailyExecutions.add(new DailyExecutionDto("Tue", 185));
        dailyExecutions.add(new DailyExecutionDto("Wed", 260));
        dailyExecutions.add(new DailyExecutionDto("Thu", 300));
        dailyExecutions.add(new DailyExecutionDto("Fri", 245));
        dailyExecutions.add(new DailyExecutionDto("Sat", 120));
        dailyExecutions.add(new DailyExecutionDto("Sun", 95));
        
        return dailyExecutions;
    }

    public List<WorkflowExecutionStatsDto> getWorkflowExecutionStats() {
        List<WorkflowExecutionStatsDto> stats = new ArrayList<>();
        
        // Get all workflows and count their executions
        List<Workflow> workflows = workflowRepository.findAll();
        int totalExecutions = workflows.size() > 0 ? 500 : 0; // Placeholder total
        
        // Sample data for demonstration
        stats.add(new WorkflowExecutionStatsDto("Invoice Processing", 175, 35.0));
        stats.add(new WorkflowExecutionStatsDto("Email Notification", 140, 28.0));
        stats.add(new WorkflowExecutionStatsDto("HR Onboarding", 85, 17.0));
        stats.add(new WorkflowExecutionStatsDto("Payment Processing", 100, 20.0));
        
        return stats;
    }

    public WorkflowStatsDto getWorkflowStats() {
        WorkflowStatsDto dto = new WorkflowStatsDto();
        
        // Count running workflows
        long runningCount = workflowRunRepository.count();
        dto.setRunning((int) Math.min(runningCount, 18)); // Placeholder
        
        // Count queued workflows
        dto.setQueued(42); // Placeholder
        
        // Count completed today
        OffsetDateTime todayStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        dto.setCompletedToday(521); // Placeholder
        
        // Count failed
        dto.setFailed(9); // Placeholder
        
        return dto;
    }

    private String findMostUsedWorkflow() {
        List<Workflow> workflows = workflowRepository.findAll();
        if (workflows.isEmpty()) {
            return "No workflows";
        }
        
        // In a real implementation, you'd query WorkflowRun to count executions per workflow
        // For now, return the first workflow name or a placeholder
        return workflows.get(0).getName() != null ? workflows.get(0).getName() : "Invoice Processing";
    }

    private double calculateAverageRuntime() {
        List<WorkflowRun> runs = workflowRunRepository.findAll();
        if (runs.isEmpty()) {
            return 0.0;
        }
        
        // Calculate average runtime in seconds
        long totalDuration = 0;
        int count = 0;
        
        for (WorkflowRun run : runs) {
            if (run.getStartedAt() != null && run.getFinishedAt() != null) {
                totalDuration += run.getFinishedAt().toEpochSecond() - run.getStartedAt().toEpochSecond();
                count++;
            }
        }
        
        return count > 0 ? (double) totalDuration / count : 3.2; // Placeholder
    }

    private String findLongestWorkflow() {
        // In a real implementation, you'd query to find the workflow with longest average execution time
        return "Invoice Approval"; // Placeholder
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
