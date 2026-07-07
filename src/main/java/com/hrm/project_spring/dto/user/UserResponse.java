package com.hrm.project_spring.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    //private String email;
    private String fullName;
    //private String status;
    //private LocalDateTime createdAt;
    private List<String> roles;
    private List<String> permissions;
}
