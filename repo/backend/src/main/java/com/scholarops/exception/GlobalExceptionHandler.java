package com.scholarops.exception;

import com.scholarops.model.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePasswordPolicyViolation(
            PasswordPolicyViolationException ex) {
        log.warn("Password policy violation detected");
        Map<String, Object> details = new HashMap<>();
        details.put("violations", ex.getViolations());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .error(ex.getMessage())
                        .data(details)
                        .build());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRateLimitExceeded(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        details.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .error(ex.getMessage())
                        .data(details)
                        .build());
    }

    @ExceptionHandler(LockedPeriodConflictException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleLockedPeriodConflict(
            LockedPeriodConflictException ex) {
        log.warn("Locked period conflict: {}", ex.getMessage());
        Map<String, Object> conflictDetails = new HashMap<>();
        conflictDetails.put("conflictingPeriodId", ex.getConflictingPeriodId());
        conflictDetails.put("conflictingPeriodTitle", ex.getConflictingPeriodTitle());
        conflictDetails.put("conflictStart", ex.getConflictStart());
        conflictDetails.put("conflictEnd", ex.getConflictEnd());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .error(ex.getMessage())
                        .data(conflictDetails)
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} field error(s)", ex.getBindingResult().getFieldErrors().size());
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .error("Validation failed")
                        .data(fieldErrors)
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
