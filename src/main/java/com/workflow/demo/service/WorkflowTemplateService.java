package com.workflow.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.demo.dto.CreateWorkflowFromTemplateRequest;
import com.workflow.demo.dto.WorkflowDto;
import com.workflow.demo.dto.WorkflowTemplateDto;
import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowTemplate;
import com.workflow.demo.entity.WorkflowVersion;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowTemplateService {

    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersioningService workflowVersioningService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowTemplateService(
            WorkflowTemplateRepository workflowTemplateRepository,
            WorkflowRepository workflowRepository,
            WorkflowVersioningService workflowVersioningService
    ) {
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.workflowRepository = workflowRepository;
        this.workflowVersioningService = workflowVersioningService;
    }

    @Transactional(readOnly = true)
    public List<WorkflowTemplateDto> listTemplates(boolean includeInactive) {
        List<WorkflowTemplate> templates = includeInactive
                ? workflowTemplateRepository.findAllByOrderByCreatedAtAsc()
                : workflowTemplateRepository.findByActiveTrueOrderByCreatedAtAsc();

        return templates.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public WorkflowTemplateDto getTemplate(UUID id) {
        WorkflowTemplate template = workflowTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        return toDto(template);
    }

    @Transactional
    public WorkflowTemplateDto createTemplate(WorkflowTemplateDto dto) {
        validateTemplateDto(dto);

        WorkflowTemplate template = new WorkflowTemplate();
        template.setName(dto.getName().trim());
        template.setCategory(dto.getCategory().trim());
        template.setDescription(dto.getDescription().trim());
        template.setSpec(dto.getSpec().trim());
        template.setActive(dto.isActive());
        template.setCreatedAt(OffsetDateTime.now());
        template.setUpdatedAt(OffsetDateTime.now());

        return toDto(workflowTemplateRepository.save(template));
    }

    @Transactional
    public WorkflowTemplateDto updateTemplate(UUID id, WorkflowTemplateDto dto) {
        validateTemplateDto(dto);

        WorkflowTemplate template = workflowTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        template.setName(dto.getName().trim());
        template.setCategory(dto.getCategory().trim());
        template.setDescription(dto.getDescription().trim());
        template.setSpec(dto.getSpec().trim());
        template.setActive(dto.isActive());
        template.setUpdatedAt(OffsetDateTime.now());

        return toDto(workflowTemplateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        if (!workflowTemplateRepository.existsById(id)) {
            throw new RuntimeException("Template not found");
        }
        workflowTemplateRepository.deleteById(id);
    }

    @Transactional
    public WorkflowDto instantiateTemplate(UUID templateId, CreateWorkflowFromTemplateRequest request, UUID ownerId) {
        WorkflowTemplate template = workflowTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (!template.isActive()) {
            throw new RuntimeException("Template is inactive");
        }

        String workflowName = (request != null && request.getWorkflowName() != null && !request.getWorkflowName().isBlank())
                ? request.getWorkflowName().trim()
                : template.getName() + " Workflow";

        String changeNote = (request != null && request.getChangeNote() != null && !request.getChangeNote().isBlank())
                ? request.getChangeNote().trim()
                : "Created from template: " + template.getName();

        boolean active = request != null && request.isActive();

        Workflow workflow = new Workflow();
        workflow.setName(workflowName);
        workflow.setOwnerId(ownerId);
        workflow.setSpec(template.getSpec());
        workflow.setActive(active);
        workflow.setUpdatedAt(OffsetDateTime.now());

        Workflow saved = workflowRepository.save(workflow);

        WorkflowVersion version = workflowVersioningService.createNewVersion(
                saved.getId(),
                template.getSpec(),
                changeNote
        );

        Workflow refreshed = workflowRepository.findByIdAndOwnerId(saved.getId(), ownerId).orElseThrow();

        WorkflowDto dto = new WorkflowDto();
        dto.setId(refreshed.getId());
        dto.setName(refreshed.getName());
        dto.setActive(refreshed.isActive());
        dto.setSpec(version.getSpec());
        dto.setActiveVersionId(version.getId());
        dto.setActiveVersionNumber(version.getVersionNumber());
        dto.setChangeNote(version.getChangeNote());
        return dto;
    }

    @Transactional
    public void seedDefaultTemplates() {
        if (workflowTemplateRepository.count() > 0) {
            return;
        }

        createSeed(
                "Webhook Logger",
                "Starter",
                "Logs payload and completes.",
                "{\"steps\":[{\"type\":\"LOG\",\"config\":{\"message\":\"Webhook received\"}}]}"
        );

        createSeed(
                "HTTP Relay",
                "Integration",
                "Forwards webhook payload to external HTTP endpoint.",
                "{\"steps\":[{\"type\":\"HTTP_CALL\",\"config\":{\"url\":\"https://httpbin.org/post\",\"method\":\"POST\"}}]}"
        );

        createSeed(
                "Audit + Notify",
                "Ops",
                "Logs start, calls API, logs completion.",
                "{\"steps\":[{\"type\":\"LOG\",\"config\":{\"message\":\"Audit start\"}},{\"type\":\"HTTP_CALL\",\"config\":{\"url\":\"https://httpbin.org/post\",\"method\":\"POST\"}},{\"type\":\"LOG\",\"config\":{\"message\":\"Audit complete\"}}]}"
        );
    }

    private void createSeed(String name, String category, String description, String spec) {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setName(name);
        template.setCategory(category);
        template.setDescription(description);
        template.setSpec(spec);
        template.setActive(true);
        template.setCreatedAt(OffsetDateTime.now());
        template.setUpdatedAt(OffsetDateTime.now());
        workflowTemplateRepository.save(template);
    }

    private void validateTemplateDto(WorkflowTemplateDto dto) {
        if (dto == null) {
            throw new RuntimeException("Template payload is required");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new RuntimeException("Template name is required");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new RuntimeException("Template category is required");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new RuntimeException("Template description is required");
        }
        if (dto.getSpec() == null || dto.getSpec().isBlank()) {
            throw new RuntimeException("Template spec is required");
        }

        try {
            objectMapper.readTree(dto.getSpec());
        } catch (Exception ex) {
            throw new RuntimeException("Template spec must be valid JSON");
        }
    }

    private WorkflowTemplateDto toDto(WorkflowTemplate template) {
        WorkflowTemplateDto dto = new WorkflowTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setCategory(template.getCategory());
        dto.setDescription(template.getDescription());
        dto.setSpec(template.getSpec());
        dto.setActive(template.isActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }
}
