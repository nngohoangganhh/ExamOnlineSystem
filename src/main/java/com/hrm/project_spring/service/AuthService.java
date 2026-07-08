package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.auth.*;
import com.hrm.project_spring.dto.user.UpdateProfileRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.entity.RefreshToken;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.RefreshTokenRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.security.CustomUserDetails;
import com.hrm.project_spring.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final EmailService emailService;
    private final JwtService jwtService;

    // ======================== ĐĂNG NHẬP (UC01) ========================
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        // Bước 1: Tìm user (tìm theo cả username lẫn email)
        User user = userRepository.findByUsernameOrEmail(
                        request.getUsernameOrEmail(),
                        request.getUsernameOrEmail()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tài khoản không tồn tại"
                ));

        // Bước 2: Kiểm tra trạng thái tài khoản TRƯỚC khi check password
        // DELETED: giả vờ không tồn tại (bảo mật – không lộ user bị xóa)
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tài khoản không tồn tại"
            );
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên."
            );
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản không hoạt động."
            );
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            // Nếu lockedUntil đã qua → tự động mở khóa
            if (user.getLockedUntil() != null &&
                    user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản tạm khóa do nhập sai nhiều lần. Vui lòng thử lại sau."
                );
            }
            // Hết thời gian khóa → mở lại
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginCount(0);
        }

        // Bước 3: Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failedCount = user.getFailedLoginCount() == null
                    ? 0
                    : user.getFailedLoginCount();
            failedCount++;
            user.setFailedLoginCount(failedCount);

            // Sau 5 lần sai → khóa tài khoản 30 phút
            if (failedCount >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userRepository.save(user);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu không đúng"
            );
        }

        // Bước 4: Kiểm tra mật khẩu quá hạn 90 ngày (BR-009)
        // passwordChangedAt mặc định = createdAt, nên user mới sẽ không bị flag ngay
        boolean requirePasswordChange =
                user.getPasswordChangedAt() == null ||
                        user.getPasswordChangedAt().isBefore(LocalDateTime.now().minusDays(90));
        user.setRequirePasswordChange(requirePasswordChange);

        // Bước 5: Tạo JWT Access Token và Refresh Token
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Bước 6: Giới hạn 3 thiết bị đồng thời (BR-004)
        // Nếu đang có >= 3 token sống → revoke cái cũ nhất trước khi cấp cái mới
        long activeTokenCount = refreshTokenRepository
                .countByUserAndRevokedFalseAndExpiresAtAfter(user, LocalDateTime.now());
        if (activeTokenCount >= 3) {
            refreshTokenRepository
                    .findTopByUserAndRevokedFalseOrderByCreatedAtAsc(user)
                    .ifPresent(oldestToken -> {
                        oldestToken.setRevoked(true);
                        refreshTokenRepository.save(oldestToken);
                    });
        }

        // Bước 7: Lưu RefreshToken mới vào DB
        // rememberMe = true → 30 ngày; false → 7 ngày
        LocalDateTime refreshTokenExpiresAt = Boolean.TRUE.equals(request.getRememberMe())
                ? LocalDateTime.now().plusDays(30)
                : LocalDateTime.now().plusDays(7);

        RefreshToken savedRefreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .revoked(false)
                .expiresAt(refreshTokenExpiresAt)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(savedRefreshToken);

        // Bước 8: Cập nhật thông tin đăng nhập thành công
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());

        // Lấy IP thực (hỗ trợ proxy/load balancer qua X-Forwarded-For)
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();
        user.setLastLoginIp(clientIp);

        userRepository.save(user);

        // Bước 9: Trả về response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .requirePasswordChange(requirePasswordChange)
                .message("Đăng nhập thành công")
                .build();
    }

    // ======================== REFRESH TOKEN ========================
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // Kiểm tra token có được gửi lên không
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không được để trống"
            );
        }

        // Kiểm tra đúng loại refresh token không
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không phải refresh token"
            );
        }

        // Lấy username từ token → tìm user
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Tài khoản không tồn tại"
                ));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Kiểm tra chữ ký JWT + hạn sử dụng
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không hợp lệ hoặc đã hết hạn"
            );
        }

        // Kiểm tra trạng thái user
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản không hoạt động"
            );
        }

        // Tìm token trong DB và kiểm tra revoked + expiresAt
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token không tồn tại trong hệ thống"
                ));

        if (Boolean.TRUE.equals(savedToken.getRevoked())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã bị thu hồi"
            );
        }

        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token đã hết hạn"
            );
        }

        // Cấp AccessToken mới, giữ nguyên RefreshToken
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .requirePasswordChange(false)
                .message("Tạo AccessToken mới thành công")
                .build();
    }

    // ======================== ĐĂNG XUẤT (UC02) ========================

    @Transactional
    public AuthResponse logout() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            SecurityContextHolder.clearContext();
            return AuthResponse.builder().message("Đăng xuất thành công").build();
        }

        // Lấy User từ SecurityContext
        Object principal = authentication.getPrincipal();
        User currentUser = null;

        if (principal instanceof CustomUserDetails userDetails) {
            currentUser = userDetails.getUser();
        } else {
            String username = authentication.getName();
            currentUser = userRepository.findByUsernameOrEmail(username, username).orElse(null);
        }

        // Xóa toàn bộ RefreshToken của user trong DB (BR-003)
        if (currentUser != null) {
            refreshTokenRepository.deleteAllByUser(currentUser);
        }

        SecurityContextHolder.clearContext();

        return AuthResponse.builder().message("Đăng xuất thành công").build();
    }

    // ======================== QUÊN MẬT KHẨU (UC03) ========================

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // Tìm user theo email nhưng KHÔNG throw exception nếu không tìm thấy
        // → Bảo mật: response luôn giống nhau dù email có hay không
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            // Tạo token ngẫu nhiên, đặt TTL 30 phút (BR-006 sửa từ 15 phút)
            String token = java.util.UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);

            // Gửi email hướng dẫn reset (nếu email tồn tại)
            emailService.sendResetPasswordEmail(user.getEmail(), token);
        });

        // Không throw, luôn im lặng – FE nhận message chung bên dưới
    }

    // ======================== RESET MẬT KHẨU (UC03) ========================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        // Tìm user theo token
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Token không hợp lệ hoặc không tồn tại"
                ));

        // Kiểm tra token còn hạn không (BR-006)
        if (user.getResetPasswordExpiry() == null ||
                user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Token đã hết hạn. Vui lòng yêu cầu gửi lại email."
            );
        }

        // Kiểm tra confirmPassword khớp newPassword
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Xác nhận mật khẩu không khớp"
            );
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setRequirePasswordChange(false);

        // Xóa token (dùng 1 lần – BR-006)
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);

        // Thu hồi TOÀN BỘ phiên đăng nhập sau khi reset (BR-007)
        // → Bảo mật: nếu tài khoản bị xâm phạm, kẻ tấn công bị kick ra khỏi tất cả thiết bị
        refreshTokenRepository.deleteAllByUser(user);
    }

    // ======================== ĐỔI MẬT KHẨU (UC04) ========================
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {

        // Lấy username từ SecurityContext
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tài khoản không tồn tại"
                ));

        // Kiểm tra mật khẩu hiện tại đúng không
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu hiện tại không chính xác"
            );
        }

        // Không cho đặt mật khẩu mới trùng mật khẩu hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu mới không được trùng mật khẩu hiện tại"
            );
        }

        // Xác nhận mật khẩu phải khớp
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Xác nhận mật khẩu không khớp"
            );
        }

        // Cập nhật mật khẩu mới và đánh dấu thời gian đổi (BR-009)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        // Thu hồi tất cả phiên KHÁC trừ phiên hiện tại (UC04 Postcondition)
        // Lấy RefreshToken hiện tại từ header Authorization hoặc body
        // → Ở đây dùng cách đơn giản: xóa toàn bộ (user phải login lại sau khi đổi mật khẩu)
        // Nếu muốn giữ phiên hiện tại, cần truyền thêm currentRefreshTokenId từ request
        refreshTokenRepository.deleteAllByUser(user);
    }

    // PROFILE
    public UserResponse getProfile() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setStatus(UserStatus.ACTIVE);
        response.setRequirePasswordChange(user.getRequirePasswordChange());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setRoles(user.getRoles().stream().map(role -> role.getCode()).toList());
        response.setPermissions(user.getRoles().stream().flatMap(role -> role.getPermissions().stream()).map(permission -> permission.getCode()).distinct().toList());
        return response;
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        return getProfile();
    }
}
