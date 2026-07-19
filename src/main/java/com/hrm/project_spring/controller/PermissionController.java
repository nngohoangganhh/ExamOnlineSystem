package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.permission.PermissionRequest;
import com.hrm.project_spring.dto.permission.PermissionResponse;
import com.hrm.project_spring.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasAuthority('PERMISSION:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PermissionResponse>>> getAllPermissions(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<PermissionResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách permission thành công")
                .data(permissionService.getAllPermissions(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('PERMISSION:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<PermissionResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiết permission")
                .data(permissionService.getPermissionById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('PERMISSION:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(@RequestBody @Valid PermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.<PermissionResponse>builder()
                .success(true)
                .code (201)
                .message("Tạo permission thành công")
                .data(permissionService.createPermission(request))
                .build());
    }

    @PreAuthorize("hasAuthority('PERMISSION:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable Long id,
            @RequestBody @Valid PermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.<PermissionResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật permission thành công")
                .data(permissionService.updatePermission(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('PERMISSION:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa permission thành công")
                .data(null)
                .build());
    }

    // ======================== Gán/Gỡ permission vào Role ========================

    @PreAuthorize("hasAuthority('PERMISSION:ASSIGN')")
    @PostMapping("/roles/{roleId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignPermissionsToRole(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
      permissionService.assignPermissionsToRole(roleId,permissionIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Gán permission cho role thành công")
                .data(null)
                .build());
    }

    @PreAuthorize("hasAuthority('PERMISSION:ASSIGN')")
    @DeleteMapping("/roles/{roleId}/remove")
    public ResponseEntity<ApiResponse<Void>> removePermissionsFromRole(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        permissionService.removePermissionsFromRole(roleId, permissionIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa permission khỏi role thành công")
                .data(null)
                .build());
    }
}
