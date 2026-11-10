package com.kasimarket.kasimarket_api.config;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

// Turns our errors into a small, safe message the screens can show to people:
// {"message": "..."}. Only text WE wrote is sent. Never a stack trace or a class name.
// The status numbers (400, 403, 409 ...) stay exactly the same as before.
@RestControllerAdvice
public class ApiExceptionHandler {

    // Every "throw new ResponseStatusException(...)" in our services ends up here.
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "The request was refused." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    // A request that breaks the rules on a request record (a blank field, a score of 9 ...).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Please check the information you entered: " + details));
    }

    // A request whose body cannot be read at all (broken JSON, a wrong type, a bad date).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "The request could not be read. Please check the information you entered."));
    }
}