package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.auth.*;
import com.hrm.project_spring.dto.user.UpdateProfileRequest;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.entity.RefreshToken;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.RefreshTokenRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    @Lazy
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateAccessToken(user);
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .message("User registered successfully")
                .build();
    }


    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không tồn tại"
                ));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không đúng");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        RefreshToken savedRefreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(savedRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .message("Đăng nhập thành công")
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshtoken();

        // 1. Check token có được gửi lên không
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không được để trống"
            );
        }

        // 2. Kiểm tra đúng loại refresh token không
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không phải refresh token"
            );
        }

        // 3. Lấy username/email từ token
        String username = jwtService.extractUsername(refreshToken);

        // 4. Tìm user
        User user =  userRepository.findByEmailOrUsername(username, username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tài khoản không tồn tại"
                ));

        // 5. Kiểm tra JWT hợp lệ không:
        // - đúng chữ ký
        // - chưa hết hạn
        // - đúng username
        // - đúng loại refresh token
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không hợp lệ hoặc đã hết hạn"
            );
        }

        // 6. Kiểm tra trạng thái user
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản không hoạt động"
            );
        }

        // 7. Tìm refresh token trong DB
        RefreshToken savedToken =  refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token không tồn tại trong hệ thống"
                ));

        // 8. Kiểm tra token đã bị thu hồi chưa
        if (Boolean.TRUE.equals(savedToken.getRevoked())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã bị thu hồi"
            );
        }

        // 9. Kiểm tra token hết hạn trong DB chưa
        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã hết hạn"
            );
        }

        // 10. Tạo access token mới
        String newAccessToken = jwtService.generateAccessToken(user);

        // 11. Trả về cho FE
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .message("Bearer")
                .build();
    }

    public UserResponse getProfile() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setRoles(user.getRoles()
                .stream()
                .map(role -> role.getCode())
                .toList());
        response.setPermissions(user.getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .toList());
        return response;
    }

    public AuthResponse logout() {
        SecurityContextHolder.clearContext();
        return AuthResponse.builder()
                .message("Logged out successfully")
                .build();
    }

    // ======================== NEW: ĐỔI MẬT KHẨU ========================

    public void changePassword(ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ======================== NEW: CẬP NHẬT PROFILE ========================

    public UserResponse updateProfile(UpdateProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        return getProfile();
    }
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        // Tạo token ngẫu nhiên
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15 phút
        userRepository.save(user);

        // Gửi email
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc không tồn tại"));

        // Kiểm tra thời hạn token
        if (user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Xóa token
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);
    }
}