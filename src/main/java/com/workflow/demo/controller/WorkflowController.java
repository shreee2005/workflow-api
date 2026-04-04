package com.workflow.demo.controller;

import com.workflow.demo.dto.WorkflowDto;
import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowVersion;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowVersionRepository;
<<<<<<< HEAD
import com.workflow.demo.service.WorkflowVersioningService;
=======
import org.springframework.transaction.annotation.Transactional;
>>>>>>> 7379d8e (Non-retry and retry)
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowVersionRepository workflowVersionRepo;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersioningService workflowVersioningService;
    private final WorkflowVersionRepository workflowVersionRepository;

<<<<<<< HEAD
    public WorkflowController(
            WorkflowRepository workflowRepository,
            WorkflowVersioningService workflowVersioningService,
            WorkflowVersionRepository workflowVersionRepository
    ) {
=======
    public WorkflowController(WorkflowVersionRepository workflowVersionRepo,
                              WorkflowRepository workflowRepository) {
        this.workflowVersionRepo = workflowVersionRepo;
>>>>>>> 7379d8e (Non-retry and retry)
        this.workflowRepository = workflowRepository;
        this.workflowVersioningService = workflowVersioningService;
        this.workflowVersionRepository = workflowVersionRepository;
    }

    @PostMapping
    @Transactional
    public WorkflowDto createWorkflow(@RequestBody WorkflowDto dto) {
        // 1) Save workflow to get ID
        Workflow wf = new Workflow();
        wf.setName(dto.getName());
        wf.setActive(dto.isActive());
<<<<<<< HEAD
        wf.setUpdatedAt(OffsetDateTime.now());

        Workflow saved = workflowRepository.save(wf);

        String note = (dto.getChangeNote() != null && !dto.getChangeNote().isBlank())
                ? dto.getChangeNote()
                : "Initial version";

        WorkflowVersion v1 = workflowVersioningService.createNewVersion(
                saved.getId(),
                dto.getSpec(),
                dto.getChangeNote() != null ? dto.getChangeNote() : "Initial version"
        );

        Workflow refreshed = workflowRepository.findById(saved.getId()).orElseThrow();
        return toDtoWithVersion(refreshed, v1);
=======
        wf = workflowRepository.save(wf);

        // 2) Create version 1 tied to this workflow
        WorkflowVersion v1 = new WorkflowVersion();
        v1.setWorkflowId(wf.getId());
        v1.setVersionNumber(1);
        v1.setSpec(wf.getSpec());
        v1 = workflowVersionRepo.save(v1);

        // 3) Set active version pointers
        wf.setActiveVersionId(v1.getId());
        wf.setActiveVersionNumber(v1.getVersionNumber());
        wf = workflowRepository.save(wf);

        return toDto(wf);
>>>>>>> 7379d8e (Non-retry and retry)
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
    public WorkflowDto updateWorkflow(@PathVariable UUID id, @RequestBody WorkflowDto dto) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        wf.setName(dto.getName());
        wf.setActive(dto.isActive());
        wf.setUpdatedAt(OffsetDateTime.now());
        workflowRepository.save(wf);

        String note = (dto.getChangeNote() != null && !dto.getChangeNote().isBlank())
                ? dto.getChangeNote()
                : "Initial version";

        WorkflowVersion newVersion = workflowVersioningService.createNewVersion(
                wf.getId(),
                dto.getSpec(),
                dto.getChangeNote() != null ? dto.getChangeNote() : "Updated from API"
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
        Workflow saved = workflowRepository.save(wf);
        return toDto(saved);
    }

    @PostMapping("/{id}/deactivate")
    public WorkflowDto deactivate(@PathVariable UUID id) {
        Workflow wf = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActive(false);
        wf.setUpdatedAt(OffsetDateTime.now());
        Workflow saved = workflowRepository.save(wf);
        return toDto(saved);
    }

    private WorkflowDto toDto(Workflow wf) {
        WorkflowDto dto = new WorkflowDto();
        dto.setId(wf.getId());
        dto.setName(wf.getName());
        dto.setActive(wf.isActive());
<<<<<<< HEAD

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

        // fallback for legacy data
        dto.setSpec(wf.getSpec());
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
=======
        dto.setActiveVersionId(wf.getActiveVersionId());
        dto.setActiveVersionNumber(wf.getActiveVersionNumber());
        return dto;
    }
>>>>>>> 7379d8e (Non-retry and retry)
}