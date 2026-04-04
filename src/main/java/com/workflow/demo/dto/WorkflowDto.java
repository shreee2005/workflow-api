// WorkflowDto.java
package com.workflow.demo.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class WorkflowDto {
    private UUID id;
    private String name;
    private String spec;   // JSON string (could contain steps, trigger, etc.)
    private boolean active;
    private UUID activeVersionId;
    private Integer activeVersionNumber;
<<<<<<< HEAD
    private String changeNote;
=======
>>>>>>> 7379d8e (Non-retry and retry)
}
