package com.jodngan.shared.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jodngan.task.application.TaskNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse("VALIDATION_ERROR", "Request body is invalid", null));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleTaskNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiErrorResponse("RESOURCE_NOT_FOUND", "Task was not found", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpectedError(Exception exception) {
        logger.error("Unexpected error while handling API request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", null));
    }
}
