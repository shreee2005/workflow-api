package com.workflow.demo.controller;

import com.workflow.demo.dto.WorkflowDto;
import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowVersion;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowVersionRepository;
import com.workflow.demo.service.WorkflowVersioningService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersioningService workflowVersioningService;
    private final WorkflowVersionRepository workflowVersionRepository;

    public WorkflowController(
            WorkflowRepository workflowRepository,
            WorkflowVersioningService workflowVersioningService,
            WorkflowVersionRepository workflowVersionRepository
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowVersioningService = workflowVersioningService;
        this.workflowVersionRepository = workflowVersionRepository;
    }

    @PostMapping
    @Transactional
    public WorkflowDto createWorkflow(@RequestBody WorkflowDto dto) {
        Workflow wf = new Workflow();
        wf.setName(dto.getName());
        wf.setActive(dto.isActive());
        wf.setSpec(dto.getSpec()); // legacy fallback field
        wf.setUpdatedAt(OffsetDateTime.now());

        Workflow saved = workflowRepository.save(wf);

        String note = (dto.getChangeNote() != null && !dto.getChangeNote().isBlank())
                ? dto.getChangeNote()
                : "Initial version";

        WorkflowVersion v1 = workflowVersioningService.createNewVersion(
                saved.getId(),
                dto.getSpec(),
                note
        );

        Workflow refreshed = workflowRepository.findById(saved.getId()).orElseThrow();
        return toDtoWithVersion(refreshed, v1);
    }

    @GetMapping
    public List<WorkflowDto> listWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtAsc()
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
    @Transactional
    public WorkflowDto updateWorkflow(@PathVariable UUID id, @RequestBody WorkflowDto dto) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        wf.setName(dto.getName());
        wf.setActive(dto.isActive());
        wf.setSpec(dto.getSpec()); // legacy fallback field
        wf.setUpdatedAt(OffsetDateTime.now());
        workflowRepository.save(wf);

        String note = (dto.getChangeNote() != null && !dto.getChangeNote().isBlank())
                ? dto.getChangeNote()
                : "Updated from API";

        WorkflowVersion newVersion = workflowVersioningService.createNewVersion(
                wf.getId(),
                dto.getSpec(),
                note
        );

        Workflow refreshed = workflowRepository.findById(wf.getId()).orElseThrow();
        return toDtoWithVersion(refreshed, newVersion);
    }

    @PostMapping("/{id}/activate")
    public WorkflowDto activate(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActive(true);
        wf.setUpdatedAt(OffsetDateTime.now());
        return toDto(workflowRepository.save(wf));
    }

    @PostMapping("/{id}/deactivate")
    public WorkflowDto deactivate(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActive(false);
        wf.setUpdatedAt(OffsetDateTime.now());
        return toDto(workflowRepository.save(wf));
    }

    private WorkflowDto toDto(Workflow wf) {
        WorkflowDto dto = new WorkflowDto();
        dto.setId(wf.getId());
        dto.setName(wf.getName());
        dto.setActive(wf.isActive());

        if (wf.getActiveVersionId() != null) {
            WorkflowVersion v = workflowVersionRepository.findById(wf.getActiveVersionId()).orElse(null);
            if (v != null) {
                dto.setActiveVersionId(v.getId());
                dto.setActiveVersionNumber(v.getVersionNumber());
                dto.setSpec(v.getSpec());
                dto.setChangeNote(v.getChangeNote());
                return dto;
            }
        }

        dto.setSpec(wf.getSpec()); // fallback for legacy rows
        dto.setActiveVersionId(wf.getActiveVersionId());
        dto.setActiveVersionNumber(wf.getActiveVersionNumber());
        return dto;
    }

    private WorkflowDto toDtoWithVersion(Workflow wf, WorkflowVersion v) {
        WorkflowDto dto = new WorkflowDto();
        dto.setId(wf.getId());
        dto.setName(wf.getName());
        dto.setActive(wf.isActive());
        dto.setSpec(v.getSpec());
        dto.setActiveVersionId(v.getId());
        dto.setActiveVersionNumber(v.getVersionNumber());
        dto.setChangeNote(v.getChangeNote());
        return dto;
    }
}
