package com.hrm.project_spring.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String roleName;// Code của role đầu tiên: ADMIN, STUDENT, ...
}
