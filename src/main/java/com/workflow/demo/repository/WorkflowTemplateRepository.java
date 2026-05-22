package com.workflow.demo.repository;

import com.workflow.demo.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {
    List<WorkflowTemplate> findAllByOrderByCreatedAtAsc();
    List<WorkflowTemplate> findByActiveTrueOrderByCreatedAtAsc();
    Optional<WorkflowTemplate> findByNameIgnoreCase(String name);
}
