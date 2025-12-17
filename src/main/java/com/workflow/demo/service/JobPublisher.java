package com.workflow.demo.service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import java.util.HashMap;
@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRun(UUID incomingEventId, UUID workflowId, String payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("incomingEventId", incomingEventId.toString());
        message.put("workflowId",      workflowId.toString());
        message.put("payload",         payload);

        rabbitTemplate.convertAndSend(
                "workflow.exchange",   // exchange name
                "workflow.run",        // routing key
                message                // will be converted to JSON
        );
    }
}


