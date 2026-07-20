package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.ExportFormat;
import com.hrm.project_spring.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserExportRequest {

    @NotNull(message = "Định dạng không được để trống")
    private ExportFormat format;

    private Boolean includeDeleted = false;

    private Set<String> fields;

    private Long roleId;

    private Long classId;

    private UserStatus status;

    private String keyword;
}