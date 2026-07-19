package com.hrm.project_spring.dto.role;

import com.hrm.project_spring.dto.permission.PermissionResponse;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean isSystem;               // FE dùng để ẩn nút Xóa/Đổi tên
    private String status;                  // "ACTIVE" / "INACTIVE"
    private int totalUser;                  // Số user đang giữ role này
    private List<PermissionResponse> permissions;
}
