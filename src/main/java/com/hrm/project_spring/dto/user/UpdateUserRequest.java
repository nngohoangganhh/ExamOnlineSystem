package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO cập nhật thông tin user — UC09.
 * BR-019: KHÔNG cho phép đổi email và username qua API này.
 * Đổi role dùng API riêng (assign/revoke role).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s\\-]+$", message = "Họ tên không được chứa ký tự đặc biệt")
    private String fullName;

    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "Số điện thoại Việt Nam không hợp lệ")
    private String phone;

    private LocalDate birthDate;

    private Gender gender;

    @Size(min = 3, max = 20, message = "Mã sinh viên phải từ 3 đến 20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã sinh viên chỉ được chứa chữ cái và số")
    private String studentCode;

    @Size(min = 3, max = 20, message = "Mã nhân viên phải từ 3 đến 20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã nhân viên chỉ được chứa chữ cái và số")
    private String employeeCode;

    /**
     * Danh sách id lớp học — tùy chọn, dùng để cập nhật lớp cho Student.
     */
    private List<Long> classIds;
}

