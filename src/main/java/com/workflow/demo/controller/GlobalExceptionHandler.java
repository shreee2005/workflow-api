package com.workflow.demo.controller;

import com.workflow.demo.dto.ApiErrorDto;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDto> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(new ApiErrorDto(status.name().toLowerCase(), message, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = mapStatus(ex.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorDto(errorCode(status), safeMessage(ex.getMessage()), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalState(IllegalStateException ex) {
        HttpStatus status = mapStatus(ex.getMessage());
        return ResponseEntity.status(status).body(new ApiErrorDto(errorCode(status), safeMessage(ex.getMessage()), null));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorDto> handleJwt(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorDto("unauthorized", "Invalid token", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorDto("internal_error", "Internal server error", Map.of("type", ex.getClass().getSimpleName())));
    }

    private HttpStatus mapStatus(String message) {
        if (message == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (message) {
            case "WORKFLOW_NOT_FOUND", "WAIT_STATE_NOT_FOUND", "team_not_found", "Workflow not found" -> HttpStatus.NOT_FOUND;
            case "WORKFLOW_NOT_ACTIVE", "Workflow is not active" -> HttpStatus.BAD_REQUEST;
            case "not_team_owner" -> HttpStatus.FORBIDDEN;
            case "unauthenticated request", "unauthenticated", "missing_authorization",
                    "invalid_token", "invalid_credentials", "no_local_password_set", "current_password_incorrect" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String safeMessage(String message) {
        return (message == null || message.isBlank()) ? "Request failed" : message;
    }

    private String errorCode(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not_found";
            case BAD_REQUEST -> "bad_request";
            default -> "error";
        };
    }
}
