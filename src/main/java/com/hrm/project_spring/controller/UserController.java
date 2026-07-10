package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.role.RoleResponse;
import com.hrm.project_spring.dto.user.*;
import com.hrm.project_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ======================== USER CRUD (Admin) ========================

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<UserResponseDto>>builder()
                        .success(true)
                        .code(200)
                        .message("Lấy danh sách thành công")
                        .data(userService.getAllUsers(pageNo, pageSize))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Lấy user theo id thành công")
                        .data(userService.getUserById(id))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo user thành công")
                        .data(userService.createUser(request))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Sửa thành công")
                        .data(userService.updateUser(id, request))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Xóa user thành công")
                        .data(null)
                        .build()
        );
    }

    // ======================== ASSIGN/REVOKE ROLE (UC06) ========================



    @PreAuthorize("hasAuthority('ROLE:UPDATE')")
    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Gán role thành công")
                        .data(userService.assignRoles(userId, request.getRoleIds()))
                        .build()
        );
    }


    @PreAuthorize("hasAuthority('ROLE:UPDATE')")
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<UserResponse>> revokeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Thu hồi role thành công")
                        .data(userService.revokeRole(userId, roleId))
                        .build()
        );
    }
}
