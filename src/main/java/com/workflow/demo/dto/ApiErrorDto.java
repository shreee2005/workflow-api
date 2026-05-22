package com.workflow.demo.dto;

import java.util.Map;

public record ApiErrorDto(
        String code,
        String message,
        Map<String, Object> details
) {
}
