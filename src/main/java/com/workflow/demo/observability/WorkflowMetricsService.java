package com.workflow.demo.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkflowMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter workflowRunsTotal;
    private final Counter workflowFailuresTotal;
    private final AtomicInteger queueBacklogSize = new AtomicInteger(0);
    private final RabbitAdmin rabbitAdmin;

    public WorkflowMetricsService(MeterRegistry meterRegistry,
                                  ObjectProvider<RabbitAdmin> rabbitAdminProvider) {
        this.meterRegistry = meterRegistry;
        this.rabbitAdmin = rabbitAdminProvider.getIfAvailable();

        this.workflowRunsTotal = Counter.builder("workflow_runs")
                .description("Total number of workflow runs triggered")
                .register(meterRegistry);

        this.workflowFailuresTotal = Counter.builder("workflow_failures")
                .description("Total number of workflow run failures")
                .register(meterRegistry);
        Gauge.builder("queue_backlog_size", queueBacklogSize, AtomicInteger::get)
                .description("Current queue backlog size")
                .register(meterRegistry);
    }

    public void incrementWorkflowRuns() {
        workflowRunsTotal.increment();
    }

    public void incrementWorkflowFailures() {
        workflowFailuresTotal.increment();
    }

    public Timer.Sample startStepTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopStepTimer(Timer.Sample sample, String stepType, String status) {
        if (sample == null) return;

        sample.stop(
                Timer.builder("step_execution_duration")
                        .description("Execution duration of workflow processing steps")
                        .tag("stepType", safe(stepType))
                        .tag("status", safe(status))
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );
    }

    public void refreshQueueBacklog(String queueName) {
        if (rabbitAdmin == null || queueName == null || queueName.isBlank()) {
            return;
        }

        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        if (info != null) {
            long messageCount = info.getMessageCount(); // primitive long
            int safeValue = (messageCount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) Math.max(0L, messageCount);
            queueBacklogSize.set(safeValue);
        }
    }

    private String safe(String value) {
        return Objects.requireNonNullElse(value, "unknown");
    }
}