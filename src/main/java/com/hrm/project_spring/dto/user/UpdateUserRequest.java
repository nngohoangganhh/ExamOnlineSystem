package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @NotNull(message = "username không đuợc để trống ")
    private String username;
    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    @NotNull(message = " Họ và tên không được để trống ")
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private String studentCode;
    private String employeeCode;

}
