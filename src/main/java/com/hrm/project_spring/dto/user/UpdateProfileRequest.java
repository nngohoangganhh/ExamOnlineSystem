package com.hrm.project_spring.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được trống")
    @Size(min=2,max = 100, message = " Họ và tên phải từ 2 đến 100 kí tự ")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được trống")
    @Size(max = 254, message = "Email không được quá 254 kí tự ")
    private String email;
}
