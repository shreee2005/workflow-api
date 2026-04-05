package com.workflow.demo.service;

import com.workflow.demo.entity.WorkflowJobMessage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @WithSpan("queue.publish")
    public void publishRun(
            @SpanAttribute("run.id") UUID runId, 
            @SpanAttribute("workflow.id") UUID workflowId, 
            @SpanAttribute("workflow.version.id") UUID workflowVersionId, 
            String payloadJson
    ) {
        WorkflowJobMessage msg = new WorkflowJobMessage();
        msg.setRunId(runId);
        msg.setWorkflowId(workflowId);
        msg.setWorkflowVersionId(workflowVersionId);
        msg.setPayload(payloadJson);
        msg.setAttempt(0);

        Span.current().setAttribute("queue.name", "workflow.tasks");
        Span.current().setAttribute("message.attempt", 0);

        System.out.println("Publishing to RabbitMQ: runId=" + runId);
        rabbitTemplate.convertAndSend("workflow.tasks", msg);

    }
}
