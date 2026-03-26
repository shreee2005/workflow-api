package com.workflow.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.entity.WorkflowRunStep;
import com.workflow.demo.entity.WorkflowWaitState;
import com.workflow.demo.repository.WorkflowRunRepository;
import com.workflow.demo.repository.WorkflowRunStepRepository;
import com.workflow.demo.repository.WorkflowWaitStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WorkflowResumeService {

    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowWaitStateRepository waitStateRepository;
    private final WorkflowRunStepRepository workflowRunStepRepository;
    private final JobPublisher jobPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowResumeService(
            WorkflowRunRepository workflowRunRepository,
            WorkflowWaitStateRepository waitStateRepository,
            WorkflowRunStepRepository workflowRunStepRepository,
            JobPublisher jobPublisher
    ) {
        this.workflowRunRepository = workflowRunRepository;
        this.waitStateRepository = waitStateRepository;
        this.workflowRunStepRepository = workflowRunStepRepository;
        this.jobPublisher = jobPublisher;
    }

    @Transactional
    public void resumeByCorrelationId(String correlationId, Map<String, Object> body) {
        WorkflowWaitState wait = waitStateRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new RuntimeException("WAIT_STATE_NOT_FOUND"));

        // idempotent: already resumed
        if (!"WAITING".equals(wait.getStatus())) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            payload = "{}";
        }

        // 1) mark wait-state resumed
        wait.setStatus("RESUMED");
        wait.setCallbackPayload(payload);
        wait.setResumedAt(OffsetDateTime.now());
        waitStateRepository.save(wait);

        // 2) mark WAIT step as SUCCEEDED (critical to unblock next step index)
        WorkflowRunStep step = workflowRunStepRepository
                .findTopByRunIdAndStepIndexOrderByStartedAtDesc(wait.getRunId(), wait.getStepIndex())
                .orElseThrow(() -> new RuntimeException("RUN_STEP_NOT_FOUND"));

        step.setStatus(WorkflowRunStep.Status.SUCCEEDED);
        step.setFinishedAt(OffsetDateTime.now());
        step.setLogs((step.getLogs() == null ? "" : step.getLogs() + "\n")
                + "Callback received correlationId=" + correlationId);
        workflowRunStepRepository.save(step);

        // 3) keep run in WAITING; worker will transition WAITING -> RUNNING on consume
        WorkflowRun run = workflowRunRepository.findById(wait.getRunId())
                .orElseThrow(() -> new RuntimeException("RUN_NOT_FOUND"));

        if (run.getStatus() != WorkflowRun.Status.WAITING) {
            run.setStatus(WorkflowRun.Status.WAITING);
            workflowRunRepository.save(run);
        }

        // 4) re-publish with callback payload as runtime input
        jobPublisher.publishRun(wait.getRunId(), wait.getWorkflowId(), wait.getWorkflowVersionId(), payload);
    }
}