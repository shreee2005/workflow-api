package com.workflow.demo.controller;

import com.workflow.demo.dto.CreateWorkflowFromTemplateRequest;
import com.workflow.demo.dto.WorkflowDto;
import com.workflow.demo.dto.WorkflowTemplateDto;
import com.workflow.demo.service.WorkflowTemplateService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class WorkflowTemplateController {

    private final WorkflowTemplateService workflowTemplateService;

    public WorkflowTemplateController(WorkflowTemplateService workflowTemplateService) {
        this.workflowTemplateService = workflowTemplateService;
    }

    @PostConstruct
    public void seedDefaults() {
        workflowTemplateService.seedDefaultTemplates();
    }

    @GetMapping
    public List<WorkflowTemplateDto> listTemplates(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return workflowTemplateService.listTemplates(includeInactive);
    }

    @GetMapping("/{id}")
    public WorkflowTemplateDto getTemplate(@PathVariable UUID id) {
        return workflowTemplateService.getTemplate(id);
    }

    @PostMapping
    public WorkflowTemplateDto createTemplate(@RequestBody WorkflowTemplateDto dto) {
        return workflowTemplateService.createTemplate(dto);
    }

    @PutMapping("/{id}")
    public WorkflowTemplateDto updateTemplate(@PathVariable UUID id, @RequestBody WorkflowTemplateDto dto) {
        return workflowTemplateService.updateTemplate(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteTemplate(@PathVariable UUID id) {
        workflowTemplateService.deleteTemplate(id);
    }

    @PostMapping("/{id}/instantiate")
    public WorkflowDto instantiateTemplate(
            @PathVariable UUID id,
            @RequestBody(required = false) CreateWorkflowFromTemplateRequest request,
            Authentication auth
    ) {
        return workflowTemplateService.instantiateTemplate(id, request, currentUserId(auth));
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
