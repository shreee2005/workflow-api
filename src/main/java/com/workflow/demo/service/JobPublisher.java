// JobPublisher.java
package com.workflow.demo.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRun(UUID incomingEventId, UUID workflowId, UUID id, String payloadJson) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("incomingEventId", incomingEventId.toString());
        msg.put("workflowId", workflowId.toString());
        msg.put("payload", payloadJson);
        rabbitTemplate.convertAndSend("workflow.tasks", msg);
    }
}
