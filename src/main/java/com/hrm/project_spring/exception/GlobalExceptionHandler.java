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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.validation.BindException;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

// (Đã fix) BUG #1: import java.util.Objects bị nhầm lẫn.
// Objects là utility class (Objects.requireNonNull...), không phải kiểu dữ liệu.
// Dùng ApiResponse<Object> (chữ O hoa, không có 's') mới đúng.

@Slf4j
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
        ex.getMostSpecificCause();
        ApiResponse<String> response = new ApiResponse<>(
                false,
                400,
                "JSON không đúng định dạng",
                ex.getMostSpecificCause().getMessage()
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
        String detail = cause.getMessage();
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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

    // Ném ra khi bind @ModelAttribute (vd query params của UserSearchRequest)
    // thất bại lúc convert kiểu, ví dụ nhập "123" vào field LocalDate createdFrom/createdTo.
    // LƯU Ý: đây là org.springframework.validation.BindException, KHÁC HOÀN TOÀN với
    // java.net.BindException (lỗi network port). Nếu import nhầm java.net.BindException,
    // handler này sẽ không bao giờ match, và lỗi sẽ rơi xuống handleGenericException()
    // bên dưới → trả về 500 thay vì 400 đúng ra phải có.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(),
                        error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Giá trị không hợp lệ: " + error.getRejectedValue())
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false, 400, "Dữ liệu tham số không hợp lệ", errors);
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
        // BUG FIX: Log để debug server-side, KHÔNG lộ stacktrace ra client
        log.error("[GlobalExceptionHandler] Unhandled exception: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = new ApiResponse<>(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Lỗi hệ thống, vui lòng thử lại sau",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Tham số '%s' có giá trị không hợp lệ: '%s'. Yêu cầu kiểu %s (ví dụ ngày: yyyy-MM-dd)",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "message", message));
    }

}