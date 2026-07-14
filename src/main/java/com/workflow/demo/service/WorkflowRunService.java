package com.workflow.demo.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.entity.WorkflowRunStep;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowRunRepository;
import com.workflow.demo.repository.WorkflowRunStepRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowRunService {

    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowRunStepRepository workflowRunStepRepository;

    public WorkflowRunService(
            WorkflowRunRepository workflowRunRepository,
            WorkflowRepository workflowRepository,
            WorkflowRunStepRepository workflowRunStepRepository
    ) {
        this.workflowRunRepository = workflowRunRepository;
        this.workflowRepository = workflowRepository;
        this.workflowRunStepRepository = workflowRunStepRepository;
    }

    public WorkflowRun createQueuedRun(UUID workflowId, UUID workflowVersionId, UUID incomingEventId) {

        WorkflowRun run = new WorkflowRun();
        run.setWorkflowId(workflowId);
        run.setWorkflowVersionId(workflowVersionId);
        run.setIncomingEventId(incomingEventId);
        run.setAttempt(0);
        run.setMaxAttempts(3);
        run.setDeadLettered(false);
        run.setStatus(WorkflowRun.Status.QUEUED);

        WorkflowRun saved = workflowRunRepository.saveAndFlush(run);
        System.out.println("API created run: " + saved.getId());
        return saved;
    }

    public List<WorkflowRun> listRunsForWorkflow(UUID workflowId, UUID userId) {
        workflowRepository.findByIdAndOwnerId(workflowId, userId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        return workflowRunRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    public WorkflowRun getRun(UUID runId, UUID userId) {
        WorkflowRun run = workflowRunRepository.findById(runId).orElseThrow();
        Workflow wf = workflowRepository.findByIdAndOwnerId(run.getWorkflowId(), userId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        if (!wf.getId().equals(run.getWorkflowId())) {
            throw new RuntimeException("Workflow not found");
        }
        return run;
    }

    public List<WorkflowRunStep> getRunSteps(UUID runId, UUID userId) {
        WorkflowRun run = workflowRunRepository.findById(runId).orElseThrow();
        // verify ownership of parent workflow
        workflowRepository.findByIdAndOwnerId(run.getWorkflowId(), userId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        return workflowRunStepRepository.findByRunIdOrderByStepIndexAsc(runId);
    }
}
