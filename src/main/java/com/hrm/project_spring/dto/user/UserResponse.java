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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
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
    private LocalDateTime lastLoginAt;
    private Boolean requirePasswordChange;
    private LocalDateTime createdAt;
    private List<String> roles;
    private List<String> permissions;
    private List<String> classCodes;

}
