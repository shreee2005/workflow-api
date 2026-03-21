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

    public WorkflowVersioningService(
            WorkflowVersionRepository workflowVersionRepository,
            WorkflowRepository workflowRepository
    ) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public WorkflowVersion createNewVersion(UUID workflowId, String spec, String changeNote) {
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
}