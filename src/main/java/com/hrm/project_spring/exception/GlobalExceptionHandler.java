package com.hrm.project_spring.exception;

import com.hrm.project_spring.dto.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String,String>>> handleValidation(
            MethodArgumentNotValidException ex){
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String,String>> response = new ApiResponse<>(false, 400, "Validation failed",errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleMalformedJson(HttpMessageNotReadableException es){
        ApiResponse<String> response = new ApiResponse<>(false, 400, "Malformed JSON role", es.getMostSpecificCause() != null ? es.getMostSpecificCause().getMessage() : es.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Objects>> handleResponseSatus(
            ResponseStatusException ex
    ){

        ApiResponse<Objects> response = new ApiResponse<>(
                false,
                ex.getStatusCode().value(),
                ex.getReason(),
                null
        );
    return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "Data integrity violation: " + ex.getMostSpecificCause().getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequest(BadRequestException be){
    ApiResponse<String> response = new ApiResponse<>(
                 false,HttpStatus.BAD_REQUEST.value(),be.getMessage(),null
    );
      return ResponseEntity.badRequest().body(response);
    }

}
