package com.workflow.demo.repository;

import com.workflow.demo.entity.WorkflowWaitState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowWaitStateRepository extends JpaRepository<WorkflowWaitState, UUID> {
    Optional<WorkflowWaitState> findByCorrelationId(String correlationId);
}