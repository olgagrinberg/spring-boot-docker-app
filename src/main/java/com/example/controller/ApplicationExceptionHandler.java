package com.example.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<String> handleValidationErrors(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", ")));
    }

    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.notFound().header("X-Error-Message", ex.getMessage()).build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMessage().contains("email") ?
                "User with this email already exists" :
                "User name is already taken";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }
}
