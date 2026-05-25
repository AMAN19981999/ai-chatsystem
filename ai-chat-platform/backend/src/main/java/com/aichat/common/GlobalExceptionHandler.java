package com.aichat.common;

import com.aichat.auth.SupabaseAuthException;
import com.aichat.auth.AuthConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SupabaseAuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleSupabaseAuthException(SupabaseAuthException exception) {
        return new ApiError(exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AuthConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleAuthConflictException(AuthConflictException exception) {
        return new ApiError(exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException exception) {
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiError(message, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        return new ApiError(exception.getMessage(), Instant.now());
    }
}
