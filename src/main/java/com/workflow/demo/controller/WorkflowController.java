// WorkflowController.java
package com.workflow.demo.controller;

import com.workflow.demo.dto.WorkflowDto;
import com.workflow.demo.entity.Workflow;
import com.workflow.demo.repository.WorkflowRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    public WorkflowController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @PostMapping
    public WorkflowDto createWorkflow(@RequestBody WorkflowDto dto) {
        Workflow wf = new Workflow();
        wf.setName(dto.getName());
        wf.setSpec(dto.getSpec());
        wf.setActive(dto.isActive());
        Workflow saved = workflowRepository.save(wf);
        return toDto(saved);
    }

    @GetMapping
    public List<WorkflowDto> listWorkflows() {
        return workflowRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public WorkflowDto getWorkflow(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        return toDto(wf);
    }

    @PutMapping("/{id}")
    public WorkflowDto updateWorkflow(@PathVariable UUID id,
                                      @RequestBody WorkflowDto dto) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setName(dto.getName());
        wf.setSpec(dto.getSpec());
        wf.setActive(dto.isActive());
        wf.setUpdatedAt(java.time.OffsetDateTime.now());
        Workflow saved = workflowRepository.save(wf);
        return toDto(saved);
    }

    @PostMapping("/{id}/activate")
    public WorkflowDto activate(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActive(true);
        wf.setUpdatedAt(java.time.OffsetDateTime.now());
        return toDto(workflowRepository.save(wf));
    }

    @PostMapping("/{id}/deactivate")
    public WorkflowDto deactivate(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActive(false);
        wf.setUpdatedAt(java.time.OffsetDateTime.now());
        return toDto(workflowRepository.save(wf));
    }

    private WorkflowDto toDto(Workflow wf) {
        WorkflowDto dto = new WorkflowDto();
        dto.setId(wf.getId());
        dto.setName(wf.getName());
        dto.setSpec(wf.getSpec());
        dto.setActive(wf.isActive());
        return dto;
    }
}
