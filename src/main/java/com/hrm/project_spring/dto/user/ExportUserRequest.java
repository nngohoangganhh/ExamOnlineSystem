package com.hrm.project_spring.dto.user;

import lombok.Data;
import java.util.List;

@Data
public class ExportUserRequest {
    // Định dạng: "xlsx" hoặc "csv"
    private String format = "xlsx";

    // Có bao gồm user đã xóa mềm không
    private boolean includeDeleted = false;

    // Các trường cần xuất (null = tất cả)
    // Ví dụ: ["username", "email", "fullName", "role", "status"]
    private List<String> fields;

    // --- Filter (theo UC13: xuất theo filter hiện tại) ---
    private String role;      // Lọc theo role: "ADMIN", "STUDENT", "TEACHER"
    private String status;    // Lọc theo trạng thái: "ACTIVE", "LOCKED"...
    private String keyword;   // Tìm theo tên hoặc email
}