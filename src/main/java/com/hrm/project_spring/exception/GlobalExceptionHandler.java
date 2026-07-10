package com.hrm.project_spring.exception;

import com.hrm.project_spring.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

// BUG #1: import java.util.Objects bị nhầm lẫn.
// Objects là utility class (Objects.requireNonNull...), không phải kiểu dữ liệu.
// Dùng ApiResponse<Object> (chữ O hoa, không có 's') mới đúng.

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trả về map field → message lỗi cụ thể từng field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>(false, 400, "Dữ liệu không hợp lệ", errors);
        return ResponseEntity.badRequest().body(response);
    }

    // BUG #2 (ĐÃ SỬA): message cũ là "Malformed JSON role" – chữ "role" là typo thừa
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleMalformedJson(HttpMessageNotReadableException ex) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                400,
                "JSON không đúng định dạng",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // VD: throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không đúng")
    // BUG #1 (ĐÃ SỬA): Kiểu cũ là ApiResponse<Objects> – Objects là java.util.Objects (utility class),
    //   không phải kiểu dữ liệu. Phải dùng ApiResponse<Object> (không có 's')
    // BUG #3 (ĐÃ SỬA): ex.getReason() trả về null khi dùng ResponseStatusException(HttpStatus, String)
    //   vì constructor đó lưu message vào getMessage(), không phải getReason().
    //   Cần dùng ex.getReason() != null ? ex.getReason() : ex.getMessage() để an toàn.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
        // getReason() → dùng khi throw new ResponseStatusException(status, reason)
        // getMessage() → fallback vì Spring tự format "status reason"
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ApiResponse<Object> response = new ApiResponse<>(
                false,
                ex.getStatusCode().value(),
                message,
                null
        );
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    // VD: insert trùng email (unique constraint) → DataIntegrityViolationException
    // BUG #4 (ĐÃ SỬA): getMostSpecificCause() có thể null nếu không có nested cause
    //   → gây NullPointerException ngay trong handler, phản tác dụng
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = (cause != null) ? cause.getMessage() : ex.getMessage();
        ApiResponse<String> response = new ApiResponse<>(
                false,
                HttpStatus.CONFLICT.value(),
                // BUG #4b: HTTP status phù hợp với duplicate/constraint violation là 409 CONFLICT, không phải 400
                "Vi phạm ràng buộc dữ liệu: " + detail,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequest(BadRequestException ex) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>(false, 400, "Dữ liệu không hợp lệ", errors);
        return ResponseEntity.badRequest().body(response);
    }

    // QUAN TRỌNG: phải khai báo TRƯỚC handler Exception.class
    // vì AccessDeniedException extends RuntimeException extends Exception
    // → nếu Exception.class đặt trước, Spring sẽ match handler đó thay vì cái này
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Object> response = new ApiResponse<>(
                false,
                HttpStatus.FORBIDDEN.value(),
                "Bạn không có quyền thực hiện hành động này",
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Tránh lộ stacktrace ra client (bảo mật)
    // Phải đặt CUỐI CÙNG
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        // In log ở server để debug, nhưng KHÔNG trả ra client
        // Nên dùng: log.error("Unhandled exception", ex); nếu có Logger
        ApiResponse<Object> response = new ApiResponse<>(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Lỗi hệ thống, vui lòng thử lại sau",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
