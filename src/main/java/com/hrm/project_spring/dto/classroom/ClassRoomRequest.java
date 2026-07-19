package com.hrm.project_spring.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassRoomRequest {
    @NotBlank(message = "Mã lớp không được để trống")
    private String code;

    @NotBlank(message = "Tên lớp không được để trống")
    private String name;

    private String description;
}
