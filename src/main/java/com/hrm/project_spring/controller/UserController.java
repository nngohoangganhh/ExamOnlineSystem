package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.*;
import com.hrm.project_spring.service.user.UserExportService;
import com.hrm.project_spring.service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserExportService userExportService;

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

    /**
     * UC08: Tạo user mới theo SRS.
     * Admin nhập thông tin, hệ thống tự sinh mật khẩu + gửi email kích hoạt.
     */
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<CreateUserResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo user thành công")
                        .data(userService.createUser(request))
                        .build()
        );
    }

    /**
     * Kích hoạt tài khoản qua activation token (từ link email).
     */
    @GetMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@RequestParam String token) {
        userService.activateUser(token);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Tài khoản đã được kích hoạt thành công. Bạn có thể đăng nhập.")
                        .build()
        );
    }

    /**
     * UC08-E3: Gửi lại email kích hoạt khi SMTP thất bại lần đầu.
     */
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping("/{id}/resend-activation")
    public ResponseEntity<ApiResponse<Void>> resendActivation(@RequestParam Long id) {
        userService.resendActivationEmail(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Email kích hoạt đã được gửi lại thành công.")
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
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

    //========================= LOCK/UNLOCK =============================

    @PreAuthorize("hasAuthority('LOCK:USER')")
    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(@PathVariable Long id, @Valid @RequestBody LockedRequest request) {
        UserResponse response = userService.lockUser(id, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message(" Khóa thành công ")
                .data(response)
                .build());
    }

    @PreAuthorize("hasAuthority('UNLOCK:USER')")
    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long id) {
        UserResponse response = userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message(" Mở khóa thành công ")
                .data(response)
                .build());
    }
    // ======================== ASSIGN/REVOKE ROLE ========================

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
                        .build());
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
    @PreAuthorize("hasAuthority('EXPORT:USER')")
    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] fileData = userExportService.exportUsers();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(fileData.length)
                .body(fileData);
    }
}
