package com.workflow.demo.service;

import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.repository.WorkflowRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowRunService {

    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowRunService(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
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

    public List<WorkflowRun> listRunsForWorkflow(UUID workflowId) {
        return workflowRunRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    public WorkflowRun getRun(UUID runId) {
        return workflowRunRepository.findById(runId).orElseThrow();
    }
}