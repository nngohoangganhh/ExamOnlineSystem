package com.hrm.project_spring.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

        @NotBlank(message = "Tên đăng nhập/email không được để trống")
        private String usernameOrEmail;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
        private String password;

        private Boolean rememberMe = false;
}
