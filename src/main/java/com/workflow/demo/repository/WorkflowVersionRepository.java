package com.workflow.demo.repository;

import com.workflow.demo.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    Optional<WorkflowVersion> findTopByWorkflowIdOrderByVersionNumberDesc(UUID workflowId);
    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberAsc(UUID workflowId);
}



