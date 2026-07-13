package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.role.RoleRequest;
import com.hrm.project_spring.dto.role.RoleResponse;
import com.hrm.project_spring.service.PermissionService;
import com.hrm.project_spring.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/roles")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @PreAuthorize("hasAuthority('ROLE:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllRoles(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .code(200)
                        .message("Lấy danh sách role thành công")
                        .data(roleService.getAllRoles(pageNo,pageSize))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('ROLE:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiết role")
                .data(roleService.getRoleById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('ROLE:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody @Valid RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo role thành công")
                .data(roleService.createRole(request))
                .build());
    }

    @PreAuthorize("hasAuthority('ROLE:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @RequestBody @Valid RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật role thành công")
                .data(roleService.updateRole(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('ROLE:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa role thành công")
                .data(null)
                .build());
    }



}
