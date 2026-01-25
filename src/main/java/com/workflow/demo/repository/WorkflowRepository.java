package com.workflow.demo.repository;

import com.workflow.demo.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow , UUID> {
    List<Workflow> findAllByOrderByCreatedAtAsc();
}
