package com.hrm.project_spring.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    @NotBlank(message = "Permission code is required")
    private String code;        // ví dụ: "EXAM:READ"

    @NotBlank(message = "Action is required")
    private String action;      // READ | CREATE | UPDATE | DELETE

    @NotBlank(message = "Permission name is required")
    private String name;        // ví dụ: "Xem kỳ thi"

    private String description;

    @NotNull(message = "Feature ID is required")
    private Long featureId;
}
