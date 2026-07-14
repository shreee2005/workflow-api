package com.workflow.demo.service;

import com.workflow.demo.entity.Workflow;
import com.workflow.demo.entity.WorkflowVersion;
import com.workflow.demo.repository.WorkflowRepository;
import com.workflow.demo.repository.WorkflowVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WorkflowVersioningService {

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowRepository workflowRepository;
    private final PluginService pluginService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public WorkflowVersioningService(
            WorkflowVersionRepository workflowVersionRepository,
            WorkflowRepository workflowRepository,
            PluginService pluginService
    ) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowRepository = workflowRepository;
        this.pluginService = pluginService;
    }

    @Transactional
    public WorkflowVersion createNewVersion(UUID workflowId, String spec, String changeNote) {
        validateSpec(spec);

        int nextVersion = workflowVersionRepository
                .findTopByWorkflowIdOrderByVersionNumberDesc(workflowId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflowId);
        version.setVersionNumber(nextVersion);
        version.setSpec(spec);
        version.setChangeNote(changeNote);
        version.setCreatedAt(OffsetDateTime.now());

        WorkflowVersion savedVersion = workflowVersionRepository.save(version);

        Workflow wf = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        wf.setActiveVersionId(savedVersion.getId());
        wf.setSpec(spec); // optional backward compatibility
        wf.setUpdatedAt(OffsetDateTime.now());
        workflowRepository.save(wf);

        return savedVersion;
    }

    private void validateSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("Workflow spec is required");
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(spec);
            com.fasterxml.jackson.databind.JsonNode stepsNode = root.get("steps");
            if (stepsNode != null && stepsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode stepNode : stepsNode) {
                    com.fasterxml.jackson.databind.JsonNode typeNode = stepNode.get("type");
                    if (typeNode != null && !typeNode.isNull()) {
                        String stepType = typeNode.asText();
                        if (pluginService.findByKey(stepType).isEmpty()) {
                            throw new IllegalArgumentException("Unsupported step type: " + stepType);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Workflow spec must be valid JSON");
        }
    }
}