package com.workflow.demo.service;

import com.workflow.demo.entity.WorkflowJobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRun(UUID runId, UUID workflowId, String payloadJson) {

        WorkflowJobMessage msg = new WorkflowJobMessage();
        msg.setRunId(runId);
        msg.setWorkflowId(workflowId);
        msg.setPayload(payloadJson);
        msg.setAttempt(0);

        rabbitTemplate.convertAndSend("workflow.tasks", msg);
    }
}
