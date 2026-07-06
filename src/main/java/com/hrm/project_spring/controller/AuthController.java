package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.auth.RefreshTokenRequest;
import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.auth.AuthResponse;
import com.hrm.project_spring.dto.auth.ChangePasswordRequest;
import com.hrm.project_spring.dto.auth.LoginRequest;
import com.hrm.project_spring.dto.user.UpdateProfileRequest;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng ký thành công")
                .data(authService.register(request))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng nhập thành công")
                .data(authService.login(request))
                .build());
    }
    @PostMapping("/refreshtoken")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request){
           return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                   .success(true)
                   .code(200)
                   .message("Tạo access Token mới thành công")
                   .data(authService.refreshToken(request))
                   .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthResponse>> logout() {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng xuất thành công")
                .data(authService.logout())
                .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy thông tin thành công")
                .data(authService.getProfile())
                .build());
    }

    /**
     * Đổi mật khẩu cho tài khoản đang đăng nhập
     * PUT /api/auth/change-password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đổi mật khẩu thành công")
                .data(null)
                .build());
    }

    /**
     * Cập nhật thông tin cá nhân (họ tên, email)
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật thông tin thành công")
                .data(authService.updateProfile(request))
                .build());
    }
}
