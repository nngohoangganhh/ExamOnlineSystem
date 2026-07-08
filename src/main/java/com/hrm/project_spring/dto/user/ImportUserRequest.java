package com.hrm.project_spring.dto.user;

import lombok.Data;

@Data
public class ImportUserRequest {
    // Mặc định true — có gửi email kích hoạt không
    private boolean sendActivationEmail = true;

    // Mặc định false — chế độ dry-run (chỉ validate, không tạo user)
    private boolean dryRun = false;

    // Role mặc định khi cột role trong file trống
    // Giá trị: "STUDENT", "TEACHER", "ADMIN"
    private String defaultRole = "STUDENT";
}