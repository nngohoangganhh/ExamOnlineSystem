package com.hrm.project_spring.dto.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterRequest {

    @NotNull(message = "Subject ID không được để trống")
    private Long subjectId;

    @NotBlank(message = "Tên chương không được để trống")
    @Size(min = 2, max = 100, message = "Tên chương phải từ 2 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Mã chương không được để trống")
    @Size(min = 2, max = 20, message = "Mã chương phải từ 2 đến 20 ký tự")
    private String code;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn hoặc bằng 1")
    private Integer order;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;
}
