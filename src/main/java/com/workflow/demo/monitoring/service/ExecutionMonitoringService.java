package com.workflow.demo.monitoring.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.entity.WorkflowRunStep;
import com.workflow.demo.monitoring.dto.ExecutionDetailDto;
import com.workflow.demo.monitoring.dto.ExecutionTimelineDto;
import com.workflow.demo.monitoring.dto.LiveExecutionDto;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import com.workflow.demo.repository.WorkflowRunStepRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExecutionMonitoringService {

    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowRunStepRepository workflowRunStepRepository;
    private final WorkflowRepository workflowRepository;

    public ExecutionMonitoringService(WorkflowRunRepository workflowRunRepository,
                                      WorkflowRunStepRepository workflowRunStepRepository,
                                      WorkflowRepository workflowRepository) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowRunStepRepository = workflowRunStepRepository;
        this.workflowRepository = workflowRepository;
    }

    public List<LiveExecutionDto> getLiveExecutions() {
        List<LiveExecutionDto> liveExecutions = new ArrayList<>();
        
        // Get running workflow runs
        List<WorkflowRun> runningRuns = workflowRunRepository.findAll().stream()
                .filter(run -> run.getStatus() == WorkflowRun.Status.RUNNING || 
                               run.getStatus() == WorkflowRun.Status.RETRYING ||
                               run.getStatus() == WorkflowRun.Status.WAITING)
                .collect(Collectors.toList());
        
        for (WorkflowRun run : runningRuns) {
            LiveExecutionDto dto = new LiveExecutionDto();
            dto.setRunId(run.getId());
            
            // Get workflow name
            Workflow workflow = workflowRepository.findById(run.getWorkflowId()).orElse(null);
            dto.setWorkflow(workflow != null ? workflow.getName() : "Unknown Workflow");
            
            // Get current step
            List<WorkflowRunStep> steps = workflowRunStepRepository.findAll().stream()
                    .filter(step -> step.getRunId().equals(run.getId()))
                    .collect(Collectors.toList());
            
            if (!steps.isEmpty()) {
                WorkflowRunStep currentStep = steps.get(steps.size() - 1);
                dto.setCurrentStep(currentStep.getStepType());
                dto.setStatus(currentStep.getStatus().name());
            } else {
                dto.setCurrentStep("Initializing");
                dto.setStatus("RUNNING");
            }
            
            // Calculate elapsed time
            if (run.getStartedAt() != null) {
                long elapsedSeconds = OffsetDateTime.now().toEpochSecond() - run.getStartedAt().toEpochSecond();
                dto.setElapsedTime(elapsedSeconds);
            } else {
                dto.setElapsedTime(0);
            }
            
            dto.setRetries(run.getAttempt());
            dto.setQueue("RabbitMQ");
            dto.setWorker("worker-" + (run.getId().hashCode() % 3 + 1)); // Simple worker assignment
            
            liveExecutions.add(dto);
        }
        
        // If no running executions, return sample data for demonstration
        if (liveExecutions.isEmpty()) {
            liveExecutions.add(createSampleLiveExecution());
        }
        
        return liveExecutions;
    }

    public ExecutionDetailDto getExecutionDetails(UUID runId) {
        ExecutionDetailDto dto = new ExecutionDetailDto();
        dto.setRunId(runId);
        
        WorkflowRun run = workflowRunRepository.findById(runId).orElse(null);
        if (run == null) {
            return dto;
        }
        
        // Get workflow name
        Workflow workflow = workflowRepository.findById(run.getWorkflowId()).orElse(null);
        dto.setWorkflowName(workflow != null ? workflow.getName() : "Unknown Workflow");
        dto.setStatus(run.getStatus().name());
        
        // Calculate total duration
        if (run.getStartedAt() != null && run.getFinishedAt() != null) {
            dto.setTotalDuration(run.getFinishedAt().toEpochSecond() - run.getStartedAt().toEpochSecond());
        } else if (run.getStartedAt() != null) {
            dto.setTotalDuration(OffsetDateTime.now().toEpochSecond() - run.getStartedAt().toEpochSecond());
        }
        
        // Get timeline of steps
        List<WorkflowRunStep> steps = workflowRunStepRepository.findAll().stream()
                .filter(step -> step.getRunId().equals(runId))
                .collect(Collectors.toList());
        
        dto.setCurrentStepIndex(steps.size());
        
        List<ExecutionTimelineDto> timeline = new ArrayList<>();
        
        // Add creation event
        timeline.add(new ExecutionTimelineDto("Created", "COMPLETED", run.getStartedAt(), "Workflow execution created"));
        
        // Add step events
        for (WorkflowRunStep step : steps) {
            String stepName = step.getStepType();
            String status = step.getStatus().name();
            OffsetDateTime timestamp = step.getStartedAt() != null ? step.getStartedAt() : OffsetDateTime.now();
            String details = step.getErrorMessage() != null ? step.getErrorMessage() : "Step executed successfully";
            
            timeline.add(new ExecutionTimelineDto(stepName, status, timestamp, details));
        }
        
        // Add completion event if finished
        if (run.getFinishedAt() != null) {
            timeline.add(new ExecutionTimelineDto("Finished", run.getStatus().name(), run.getFinishedAt(), 
                    run.getStatus() == WorkflowRun.Status.SUCCEEDED ? "Workflow completed successfully" : "Workflow failed"));
        }
        
        dto.setTimeline(timeline);
        
        return dto;
    }

    private LiveExecutionDto createSampleLiveExecution() {
        LiveExecutionDto dto = new LiveExecutionDto();
        dto.setRunId(UUID.randomUUID());
        dto.setWorkflow("Invoice Processing");
        dto.setCurrentStep("Approval");
        dto.setElapsedTime(18);
        dto.setRetries(0);
        dto.setQueue("RabbitMQ");
        dto.setWorker("worker-2");
        dto.setStatus("RUNNING");
        return dto;
    }
}
