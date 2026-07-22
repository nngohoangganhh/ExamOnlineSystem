package com.hrm.project_spring.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassRoomRequest {

    @NotBlank(message = "Mã lớp không được để trống")
    @Size(min = 3, max = 20, message = "Mã lớp phải từ 3 đến 20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "Mã lớp chỉ được chứa chữ cái, số và dấu gạch ngang")
    private String code;

    @NotBlank(message = "Tên lớp không được để trống")
    @Size(min = 2, max = 100, message = "Tên lớp phải từ 2 đến 100 ký tự")
    private String name;

    @Pattern(
            regexp = "^(20\\d{2})-(20\\d{2})$",
            message = "Năm học phải đúng định dạng YYYY-YYYY (ví dụ: 2025-2026)"
    )
    private String academicYear;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    /**
     * UC15: Id giáo viên chủ nhiệm — tùy chọn.
     * Phải là user tồn tại và có role TEACHER.
     */
    private Long teacherId;
}

