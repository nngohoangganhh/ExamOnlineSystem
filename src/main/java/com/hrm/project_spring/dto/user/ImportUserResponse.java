package com.hrm.project_spring.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportUserResponse {
    private int totalRows;         // Tổng số dòng
    private int successCount;      // Số dòng thành công
    private int skipCount;         // Số dòng bỏ qua (trùng)
    private int errorCount;        // Số dòng lỗi
    private boolean dryRun;        // Có phải dry-run không
    private List<ImportUserRowResult> details; // Chi tiết từng dòng
}