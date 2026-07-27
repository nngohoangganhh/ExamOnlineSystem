package com.hrm.project_spring.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportUserRowResult {
    private int rowNumber;        // Số thứ tự dòng
    private String status;        // "success", "skip", "error"
    private String message;       // Lý do lỗi (nếu có)
    private String username;      // Username của dòng đó
    private String email;         // Email của dòng đó
}