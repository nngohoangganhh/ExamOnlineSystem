package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO tạo user theo SRS UC08.
 * Admin KHÔNG nhập mật khẩu — hệ thống tự sinh.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Họ tên không được trống")
    @Size(min = 2, max = 100, message = "Họ tên 2-100 ký tự, không chứa ký tự đặc biệt.")
    @Pattern(regexp = "^[\\p{L}\\s\\-]+$", message = "Họ tên 2-100 ký tự, không chứa ký tự đặc biệt.")
    private String fullName;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ hoặc đã tồn tại.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    @NotBlank(message = "Username không được trống")
    @Size(min = 3, max = 50, message = "Username 3-50 ký tự (a-z, 0-9, ._-) và không trống.")
    @Pattern(regexp = "^[a-zA-Z0-9._\\-]+$", message = "Username 3-50 ký tự (a-z, 0-9, ._-) và không trống.")
    private String username;

    // Mã số nhân viên hoặc sinh viên (tùy role)
    @Size(min = 3, max = 20, message = "Mã số không hợp lệ hoặc đã tồn tại.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã số không hợp lệ hoặc đã tồn tại.")
    private String studentCode;

    @Size(min = 3, max = 20, message = "Mã số không hợp lệ hoặc đã tồn tại.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Mã số không hợp lệ hoặc đã tồn tại.")
    private String employeeCode;

    @NotNull(message = "Role không hợp lệ.")
    private Long roleIds;

    // Bắt buộc nếu role = Student
    private Long classIds;

    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "Số điện thoại Việt Nam không hợp lệ.")
    private String phone;

    private LocalDate birthDate;

    private Gender gender;

    /**
     * Nếu true: bỏ qua email kích hoạt, set Active ngay (Alternate flow A2).
     * Mặc định: false → status = Pending + gửi email kích hoạt.
     */
    private Boolean skipActivation = false;
}
