package com.hrm.project_spring.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotBlank(message = "Mã role không được để trống")
    @Size(min = 2, max = 50, message = "Mã role phải từ 2 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Mã role chỉ chấp nhận chữ cái, số và dấu gạch dưới")
    private String code;

    @NotBlank(message = "Tên role không được để trống")
    @Size(min = 2, max = 50, message = "Tên role phải từ 2 đến 50 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    private Set<Long> permissionIds;    // ID các permissions được gán
}
