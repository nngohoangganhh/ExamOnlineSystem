package com.hrm.project_spring.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(min = 2, max = 100, message = "Tên môn học phải từ 2 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Mã môn học không được để trống")
    @Size(min = 2, max = 20, message = "Mã môn học phải từ 2 đến 20 ký tự")
    private String code;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;
}
