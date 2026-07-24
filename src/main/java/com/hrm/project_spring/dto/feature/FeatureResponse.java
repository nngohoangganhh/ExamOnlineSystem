package com.hrm.project_spring.dto.feature;

import com.hrm.project_spring.dto.permission.PermissionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<PermissionResponse> permissions;
}
