package com.craftbook.api;

import com.craftbook.calculator.domain.InvalidCalculationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCalculationException.class)
    public ResponseEntity<ApiError> invalidCalculation(InvalidCalculationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage(), exception.violations()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Requisição inválida", errors));
    }

    public record ApiError(String message, List<String> errors) { }
}
