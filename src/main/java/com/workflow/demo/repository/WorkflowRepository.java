package com.workflow.demo.repository;

import com.workflow.demo.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow , UUID> {
}
