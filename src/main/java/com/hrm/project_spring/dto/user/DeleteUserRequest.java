package com.hrm.project_spring.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO xóa user — UC11.
 * SRS yêu cầu admin phải nhập lý do và xác nhận tên user trước khi xóa.
 */
@Data
public class DeleteUserRequest {

    /**
     * Lý do xóa — bắt buộc, 10-500 ký tự.
     */
    @NotBlank(message = "Lý do xóa không được để trống")
    @Size(min = 10, max = 500, message = "Lý do xóa phải từ 10 đến 500 ký tự")
    private String reason;

    /**
     * Tên đăng nhập của user bị xóa — admin phải gõ đúng để xác nhận (E2).
     */
    @NotBlank(message = "Tên xác nhận không được để trống")
    private String confirmName;
}
