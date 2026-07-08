package com.hrm.project_spring.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    // Tên field đổi từ oldPassword → currentPassword cho đúng ngữ nghĩa SRS UC04
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 64, message = "Mật khẩu mới phải từ 8 đến 64 ký tự")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+]).+$",
        message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"
    )
    private String newPassword;

    // Xác nhận mật khẩu – kiểm tra khớp với newPassword tại service layer
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}
