package com.workflow.demo.repository;

import com.workflow.demo.entity.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    List<WorkflowRun> findByWorkflowIdOrderByStartedAtDesc(UUID workflowId);
}
