package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.auth.*;
import com.hrm.project_spring.dto.common.ApiResponse;
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

    /**
     * POST /api/auth/login
     * Đăng nhập bằng username hoặc email + password.
     * Trả về accessToken + refreshToken.
     */
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

    /**
     * POST /api/auth/refresh-token
     * Cấp AccessToken mới từ RefreshToken hợp lệ.
     * Không yêu cầu xác thực (vì AccessToken đã hết hạn).
     */
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

    /**
     * POST /api/auth/logout
     * Đăng xuất: thu hồi RefreshToken trong DB, clear SecurityContext.
     * Yêu cầu: gửi kèm AccessToken hợp lệ trong header Authorization.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đăng xuất thành công")
                .data(null)
                .build());
    }

    /**
     * POST /api/auth/forgot-password
     * Gửi email hướng dẫn reset mật khẩu.
     * Luôn trả HTTP 200 dù email có tồn tại hay không (bảo mật – SRS UC03).
     */
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

    /**
     * POST /api/auth/reset-password
     * Đặt lại mật khẩu bằng token nhận qua email.
     * Không yêu cầu xác thực (user chưa đăng nhập được).
     */
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

    /**
     * PUT /api/auth/change-password
     * Đổi mật khẩu khi đang đăng nhập.
     * Yêu cầu: AccessToken hợp lệ trong header Authorization.
     */
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
}
