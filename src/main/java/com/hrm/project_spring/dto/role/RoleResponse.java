package com.hrm.project_spring.dto.role;

import com.hrm.project_spring.dto.permission.PermissionResponse;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private List<PermissionResponse> permissions;
}
