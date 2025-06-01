package com.taskapp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private static final String SSE_ENDPOINT_PATH = "/api/notifications/stream";
    
    // Utility method to check if request is for SSE endpoint
    private boolean isSSEEndpoint(WebRequest request) {
        return request.getDescription(false).contains(SSE_ENDPOINT_PATH);
    }
    
    // Utility method to extract path from request
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    // Generic error response builder
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, WebRequest request, HttpStatus status, String error, String message) {
        if (isSSEEndpoint(request)) {
            log.debug("Skipping exception handling for SSE endpoint: {}", ex.getMessage());
            return null;
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .message(message)
            .path(extractPath(request))
            .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler({Exception.class, HttpMessageNotWritableException.class})
    public ResponseEntity<ErrorResponse> handleGenericExceptions(Exception ex, WebRequest request) {
        if (ex instanceof HttpMessageNotWritableException) {
            log.warn("Message not writable: {}", ex.getMessage());
            return buildErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, 
                "Serialization Error", "Failed to serialize response");
        }
        
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error", "An unexpected error occurred");
    }    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler({UsernameNotFoundException.class, TaskNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(Exception ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        String errorType = ex instanceof UsernameNotFoundException ? "User Not Found" : 
                          ex instanceof TaskNotFoundException ? "Task Not Found" : "User Not Found";
        return buildErrorResponse(ex, request, HttpStatus.NOT_FOUND, errorType, ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationExceptions(Exception ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        String message = ex instanceof BadCredentialsException ? "Invalid username or password" : ex.getMessage();
        return buildErrorResponse(ex, request, HttpStatus.UNAUTHORIZED, "Authentication Failed", message);
    }

    @ExceptionHandler({AccessDeniedException.class, SecurityException.class, TaskPermissionException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedExceptions(Exception ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        String message = ex instanceof AccessDeniedException ? "You don't have permission to access this resource" : ex.getMessage();
        String errorType = ex instanceof TaskPermissionException ? "Permission Denied" : "Access Denied";
        return buildErrorResponse(ex, request, HttpStatus.FORBIDDEN, errorType, message);
    }    @ExceptionHandler({DataIntegrityViolationException.class, TaskAssignmentException.class})
    public ResponseEntity<ErrorResponse> handleConflictExceptions(Exception ex, WebRequest request) {
        log.warn("Conflict error: {}", ex.getMessage());
        
        if (ex instanceof DataIntegrityViolationException) {
            String message = "Data integrity violation";
            if (ex.getMessage() != null) {
                if (ex.getMessage().contains("username")) {
                    message = "Username is already taken";
                } else if (ex.getMessage().contains("email")) {
                    message = "Email is already in use";
                }
            }
            return buildErrorResponse(ex, request, HttpStatus.CONFLICT, "Conflict", message);
        }
        
        // TaskAssignmentException
        return buildErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "Assignment Error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .path(extractPath(request))
            .validationErrors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
