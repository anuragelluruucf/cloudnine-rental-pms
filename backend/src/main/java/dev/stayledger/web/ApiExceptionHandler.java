package dev.stayledger.web;

import dev.stayledger.service.BookingConflictException;
import dev.stayledger.web.Dtos.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(BookingConflictException.class)
    ResponseEntity<ApiError> overlap(BookingConflictException ex) {
        return ResponseEntity.status(409).body(new ApiError("OVERLAP", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .findFirst()
                .orElse("Check the form and try again.");
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION", message));
    }
}
