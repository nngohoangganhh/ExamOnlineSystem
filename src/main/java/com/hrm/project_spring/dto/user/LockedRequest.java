package com.hrm.project_spring.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LockedRequest {

    /**
     * Lý do khóa tài khoản — bắt buộc, 10-500 ký tự (BR-021).
     */
    @NotBlank(message = "Lý do khóa tài khoản không được để trống")
    @Size(min = 10, max = 500, message = "Lý do khóa phải từ 10 đến 500 ký tự")
    private String reason;

    /**
     * Thời hạn khóa — tùy chọn.
     * Nếu có: phải sau thời điểm hiện tại ít nhất 1 giờ và không quá 5 năm.
     * Validation chi tiết được thực hiện trong UserService.lockUser().
     */
    private LocalDateTime lockUntil;
}

