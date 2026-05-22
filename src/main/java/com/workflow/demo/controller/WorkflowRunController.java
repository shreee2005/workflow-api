package com.workflow.demo.controller;

import com.workflow.demo.entity.WorkflowRun;
import com.workflow.demo.service.WorkflowRunService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public List<WorkflowRun> listRuns(@PathVariable UUID workflowId, Authentication auth) {
        return workflowRunService.listRunsForWorkflow(workflowId, currentUserId(auth));
    }

    @GetMapping("/runs/{runId}")
    public WorkflowRun getRun(@PathVariable UUID runId, Authentication auth) {
        return workflowRunService.getRun(runId, currentUserId(auth));
    }

    private UUID currentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        if (auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unexpected_principal_type");
    }
}
