package com.telcocrm.orderservice.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(BaseException ex) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : "UNKNOWN_ERROR";
        log.warn("Business exception [{}]: {}", errorCode, ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create("https://telcocrm.com/errors/" + errorCode.toLowerCase(Locale.ROOT).replace("_", "-")));
        problem.setTitle(errorCode);
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, key -> new ArrayList<>()).add(message);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problem.setType(URI.create("https://telcocrm.com/errors/validation-error"));
        problem.setTitle("VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problem.setType(URI.create("https://telcocrm.com/errors/validation-error"));
        problem.setTitle("VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '" + ex.getName() + "' has invalid value: " + ex.getValue();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        problem.setType(URI.create("https://telcocrm.com/errors/invalid-parameter"));
        problem.setTitle("INVALID_PARAMETER");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadableException(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setType(URI.create("https://telcocrm.com/errors/malformed-request"));
        problem.setTitle("MALFORMED_REQUEST");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Order was updated by another operation, please retry"
        );
        problem.setType(URI.create("https://telcocrm.com/errors/concurrent-update"));
        problem.setTitle("CONCURRENT_UPDATE");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://telcocrm.com/errors/illegal-state"));
        problem.setTitle("ILLEGAL_STATE");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ProblemDetail> handleFeignException(FeignException ex) {
        log.error("Feign call failed: {}", ex.getMessage());
        HttpStatus status = ex instanceof FeignException.NotFound ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Upstream service call failed");
        problem.setType(URI.create("https://telcocrm.com/errors/upstream-service-error"));
        problem.setTitle("UPSTREAM_SERVICE_ERROR");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error occurred"
        );
        problem.setType(URI.create("https://telcocrm.com/errors/internal-server-error"));
        problem.setTitle("INTERNAL_SERVER_ERROR");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.internalServerError().body(problem);
    }
}
