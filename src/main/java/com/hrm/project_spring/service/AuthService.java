package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.auth.AuthResponse;
import com.hrm.project_spring.dto.auth.ChangePasswordRequest;
import com.hrm.project_spring.dto.auth.LoginRequest;
import com.hrm.project_spring.dto.auth.RefreshTokenRequest;
import com.hrm.project_spring.dto.user.UpdateProfileRequest;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.entity.RefreshToken;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.RefreshTokenRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.security.JwtService;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

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

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshtoken();
        // 1. Kiểm tra JWT hợp lệ không
        if (!jwtService.isRefreshToken(refreshToken)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED," RefeshToken không hợp lệ ");
        }
        // 2. Kiểm tra đúng loại refresh token không


        // 3. Lấy username từ token
        // 4. Tìm user
        // 5. Kiểm tra trạng thái user
        // 6. Tìm refresh token trong DB
        // 7. Kiểm tra token đã bị thu hồi chưa
        // 8. Kiểm tra token hết hạn chưa
        // 9. Tạo access token mới
        // 10. Trả về cho FE
        return null;
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
}
