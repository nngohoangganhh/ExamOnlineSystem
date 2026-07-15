package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.Gender;
import com.hrm.project_spring.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response khi tạo user theo UC08.
 * Bao gồm generatedPassword nếu Admin chọn skipActivation (flow A2)
 * → hiển thị 1 lần duy nhất cho Admin copy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
    private String studentCode;
    private String employeeCode;
    private UserStatus status;
    private LocalDateTime createdAt;
    private List<String> roles;

    /**
     * Chỉ trả về khi skipActivation = true (flow A2).
     * Hiển thị 1 lần duy nhất cho Admin copy.
     * null nếu gửi email kích hoạt bình thường.
     */
    private String generatedPassword;

    private String activationMessage;
}
