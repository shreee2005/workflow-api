package com.workflow.demo.controller;

import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.service.WorkflowRunService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WorkflowRunController {

    private final WorkflowRunService workflowRunService;

    public WorkflowRunController(WorkflowRunService workflowRunService) {
        this.workflowRunService = workflowRunService;
    }

    @GetMapping("/workflows/{workflowId}/runs")
    public List<WorkflowRun> listRuns(@PathVariable UUID workflowId) {
        return workflowRunService.listRunsForWorkflow(workflowId);
    }

    @GetMapping("/runs/{runId}")
    public WorkflowRun getRun(@PathVariable UUID runId) {
        return workflowRunService.getRun(runId);
    }
}
