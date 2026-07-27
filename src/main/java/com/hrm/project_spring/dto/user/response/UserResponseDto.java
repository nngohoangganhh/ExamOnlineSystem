package com.hrm.project_spring.dto.user.response;

import com.hrm.project_spring.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private List<String> roleNames;// Code của role đầu tiên: ADMIN, STUDENT, ...
    private UserStatus status;
    private List<String> classCodes;
}
