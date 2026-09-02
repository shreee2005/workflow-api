package com.workflow.demo.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class HttpAppender extends AppenderBase<ILoggingEvent> {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private String url;
    private String serviceId;
    private String apiKey;
    private String format = "JSON";

    public void setUrl(String url) { this.url = url; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setFormat(String format) { this.format = format; }

    @Override
    public void start() {
        if (url == null || url.isBlank() || apiKey == null || apiKey.isBlank()) {
            addError("HttpAppender requires both url and apiKey.");
            return;
        }
        if (serviceId == null || serviceId.isBlank()) {
            serviceId = "unknown-service";
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;

        try {
            Map<String, String> mdc = event.getMDCPropertyMap();
            String traceId = firstNonBlank(
                    mdc.get("traceId"), mdc.get("trace_id"), mdc.get("requestId"));
            String spanId = firstNonBlank(mdc.get("spanId"), mdc.get("span_id"));

            String timestamp = Instant.ofEpochMilli(event.getTimeStamp()).toString();
            String message = event.getFormattedMessage();
            String stackTrace = throwableToString(event.getThrowableProxy());
            if (stackTrace != null && !stackTrace.isBlank()) {
                message += "\n" + stackTrace;
            }

            // The platform's JSON parser expects message itself to be JSON.
            String logEvent = "{\"message\":\"" + escapeJson(message) + "\","
                    + "\"level\":\"" + escapeJson(event.getLevel().toString()) + "\","
                    + "\"timestamp\":\"" + timestamp + "\""
                    + optionalJsonField("traceId", traceId)
                    + optionalJsonField("spanId", spanId)
                    + "}";

            String requestBody = "{\"serviceId\":\"" + escapeJson(serviceId) + "\","
                    + "\"level\":\"" + escapeJson(event.getLevel().toString()) + "\","
                    + "\"format\":\"JSON\","
                    + "\"message\":\"" + escapeJson(logEvent) + "\","
                    + "\"timestamp\":\"" + timestamp + "\""
                    + optionalJsonField("traceId", traceId)
                    + optionalJsonField("spanId", spanId)
                    + "}";

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 400) {
                            addWarn("Log Analyzer rejected log: HTTP " + response.statusCode());
                        }
                    })
                    .exceptionally(error -> {
                        addError("Could not send log to Log Analyzer", error);
                        return null;
                    });
        } catch (Exception error) {
            addError("Could not prepare log for Log Analyzer", error);
        }
    }

    private static String optionalJsonField(String name, String value) {
        return value == null ? "" : ",\"" + name + "\":\"" + escapeJson(value) + "\"";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String throwableToString(IThrowableProxy throwable) {
        if (throwable == null) return null;

        StringBuilder result = new StringBuilder()
                .append(throwable.getClassName())
                .append(": ")
                .append(throwable.getMessage());

        for (StackTraceElementProxy element : throwable.getStackTraceElementProxyArray()) {
            result.append("\n\tat ").append(element.getStackTraceElement());
        }
        return result.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}