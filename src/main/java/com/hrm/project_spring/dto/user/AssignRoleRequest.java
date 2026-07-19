package com.hrm.project_spring.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRoleRequest {

    @NotEmpty(message = "Phải chọn ít nhất 1 role")
    private List<Long> roleIds;
}
