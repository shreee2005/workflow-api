package com.workflow.demo.service;

import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowRunService {

    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    public WorkflowRun createQueuedRun(UUID workflowId, UUID incomingEventId) {
        WorkflowRun run = new WorkflowRun();
        run.setWorkflowId(workflowId);
        run.setIncomingEventId(incomingEventId);
        run.setAttempt(0);
        run.setMaxAttempts(3);
        run.setStatus(WorkflowRun.Status.QUEUED);
        run.setStartedAt(null);
        run.setFinishedAt(null);
        run.setErrorMessage(null);
        return workflowRunRepository.save(run);
    }

    public List<WorkflowRun> listRunsForWorkflow(UUID workflowId) {
        return workflowRunRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    public WorkflowRun getRun(UUID runId) {
        return workflowRunRepository.findById(runId).orElseThrow();
    }

    // Optional: helper to update timestamps if you show them in API
    public WorkflowRun touchStarted(UUID runId) {
        WorkflowRun run = getRun(runId);
        if (run.getStartedAt() == null) {
            run.setStartedAt(OffsetDateTime.now());
            return workflowRunRepository.save(run);
        }
        return run;
    }
}
