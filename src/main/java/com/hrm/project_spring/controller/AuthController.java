package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.auth.*;
import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.user.UpdateProfileRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng nhập thành công")
                .data(authService.login(request, httpServletRequest))
                .build());
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Tạo AccessToken mới thành công")
                .data(authService.refreshToken(request))
                .build());
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đăng xuất thành công")
                .data(null)
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                // Message chung – không tiết lộ email có tồn tại hay không
                .message("Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu trong vài phút")
                .data(null)
                .build());
    }


    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.")
                .data(null)
                .build());
    }
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.changePassword(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đổi mật khẩu thành công. Vui lòng đăng nhập lại trên các thiết bị khác.")
                .data(null)
                .build());
    }
    // GetProfile
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("lấy thông tin cá nhân thành công")
                .data(authService.getProfile())
                .build());
    }
    //UpdateProfile
    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhận thông tin thành công ")
                .data(authService.updateProfile(request))
                .build()
        );
    }
}
