package com.telcocrm.billingservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "detail", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "detail", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(OutboxPersistenceException.class)
    public ResponseEntity<Map<String, Object>> handleOutboxError(OutboxPersistenceException ex) {
        log.error("Outbox persistence failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "detail", "Internal error processing event",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "detail", "You do not have permission to perform this action",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "detail", "An unexpected error occurred",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
