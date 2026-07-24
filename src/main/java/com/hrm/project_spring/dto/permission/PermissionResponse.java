package com.hrm.project_spring.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private Long id;
    private String code;        // ví dụ: "EXAM:READ"
    private String action;      // ví dụ: "READ"
    private String name;
    private String description;
    private Long featureId;
    private String featureName;
    private String featureCode;
}
