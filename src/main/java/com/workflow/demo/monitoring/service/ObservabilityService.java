package com.workflow.demo.monitoring.service;

import com.workflow.demo.monitoring.dto.MetricsUpdateDto;
import com.workflow.demo.monitoring.dto.ObservabilityLinksDto;
import com.workflow.demo.monitoring.dto.RecentTraceDto;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ObservabilityService {

    // These would typically come from application properties
    private static final String GRAFANA_URL = "http://localhost:3000";
    private static final String ZIPKIN_URL = "http://localhost:9411";
    private static final String PROMETHEUS_URL = "http://localhost:9090";

    public ObservabilityLinksDto getObservabilityLinks() {
        ObservabilityLinksDto dto = new ObservabilityLinksDto();
        dto.setGrafanaUrl(GRAFANA_URL);
        dto.setZipkinUrl(ZIPKIN_URL);
        dto.setPrometheusUrl(PROMETHEUS_URL);
        return dto;
    }

    public List<RecentTraceDto> getRecentTraces() {
        List<RecentTraceDto> traces = new ArrayList<>();
        
        // Sample data - in production, this would query Zipkin or OpenTelemetry
        traces.add(new RecentTraceDto("Payment Processing", 1800, "abcd123", "Success", 
                OffsetDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        traces.add(new RecentTraceDto("Invoice Approval", 3200, "efgh456", "Success", 
                OffsetDateTime.now().minusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        traces.add(new RecentTraceDto("Email Notification", 450, "ijkl789", "Failed", 
                OffsetDateTime.now().minusMinutes(15).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        traces.add(new RecentTraceDto("HR Onboarding", 5400, "mnop012", "Success", 
                OffsetDateTime.now().minusMinutes(20).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        traces.add(new RecentTraceDto("Data Sync", 890, "qrst345", "Success", 
                OffsetDateTime.now().minusMinutes(25).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        
        return traces;
    }

    public MetricsUpdateDto getMetricsUpdateInfo() {
        MetricsUpdateDto dto = new MetricsUpdateDto();
        dto.setLastUpdated(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        dto.setSecondsAgo(12);
        return dto;
    }
}
