package com.example.auth_service.exception;

import com.example.auth_service.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {

        // Clean the message by removing status code prefix if present
        String errorMessage = ex.getReason();
        if (errorMessage != null && errorMessage.startsWith(ex.getStatusCode().toString())) {
            errorMessage = errorMessage.replaceFirst("^\\d+\\s\\w+\\s", "").trim();
        }

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(ex.getStatusCode().value())
                        .reason(getErrorName(ex.getStatusCode()))
                        .message(errorMessage)
                        .path(getRequestPath(request))
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .reason(getErrorName(HttpStatus.BAD_REQUEST))
                        .message(errorMessage)
                        .path(getRequestPath(request))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(
            Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred", ex);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.<Void>builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .reason(getErrorName(HttpStatus.INTERNAL_SERVER_ERROR))
                        .message("Internal server error")
                        .path(getRequestPath(request))
                        .build());
    }

    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getErrorName(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus status) {
            return status.getReasonPhrase();
        }
        return statusCode.toString();
    }
}