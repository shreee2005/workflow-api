package com.workflow.demo.repository;

import com.workflow.demo.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

<<<<<<< HEAD
import java.util.List;
=======
>>>>>>> 7379d8e (Non-retry and retry)
import java.util.Optional;
import java.util.UUID;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    Optional<WorkflowVersion> findTopByWorkflowIdOrderByVersionNumberDesc(UUID workflowId);
<<<<<<< HEAD
    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberAsc(UUID workflowId);
}
=======
}
>>>>>>> 7379d8e (Non-retry and retry)
