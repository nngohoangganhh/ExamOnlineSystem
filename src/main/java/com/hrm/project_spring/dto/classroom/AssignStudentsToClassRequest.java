package com.hrm.project_spring.dto.classroom;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsToClassRequest {
    @NotEmpty(message = "Danh sách sinh viên không được để trống")
    private List<Long> studentIds;
}
