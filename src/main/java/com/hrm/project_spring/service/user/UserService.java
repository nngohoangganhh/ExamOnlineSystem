package com.hrm.project_spring.service.user;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.*;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.ClassRoomRepository;
import com.hrm.project_spring.repository.RefreshTokenRepository;
import com.hrm.project_spring.repository.RoleRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClassRoomRepository classRoomRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ======================== LẤY DANH SÁCH USER ========================
    @Transactional
    public PageResponse<UserResponseDto> getAllUsers(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<User> users = userRepository.findAll(pageable);
        List<UserResponseDto> content =
                users.getContent()
                        .stream()
                        .map(this::mapTo)
                        .collect(Collectors.toList());
        return PageResponse.<UserResponseDto>builder()
                .content(content)
                .pageNo(users.getNumber())
                .pageSize(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    // ======================== LẤY USER THEO ID ========================
    @Transactional
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        return mapToResponse(user);
    }

    // ======================== TẠO USER (UC08 - SRS) ========================

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {

        // === 1. Validate unique email & username (kể cả soft deleted - BR-017) ===
        if (userRepository.existsByEmailIncludingDeleted(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi user khác");
        }
        if (userRepository.existsByUsernameIncludingDeleted(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng");
        }

        // === 2. Validate role tồn tại ===
        Role role = roleRepository.findById(request.getRoleIds())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role không hợp lệ."));

        // === 3. Nếu role = STUDENT → classId bắt buộc ===
        ClassRoom classRoom = null;
        if ("STUDENT".equalsIgnoreCase(role.getCode())) {
            if (request.getClassIds() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lớp học không hợp lệ. ClassId bắt buộc khi role là Student.");
            }
            classRoom = classRoomRepository.findById(request.getClassIds())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lớp học không hợp lệ."));
        }

        // === 4. Validate studentCode / employeeCode unique ===
        if (request.getStudentCode() != null && !request.getStudentCode().isBlank()) {
            if (userRepository.existsByStudentCode(request.getStudentCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã số không hợp lệ hoặc đã tồn tại.");
            }
        }
        if (request.getEmployeeCode() != null && !request.getEmployeeCode().isBlank()) {
            if (userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã số không hợp lệ hoặc đã tồn tại.");
            }
        }

        // === 5. Validate birthDate (1920 đến currentYear - 5) ===
        if (request.getBirthDate() != null) {
            int currentYear = LocalDate.now().getYear();
            int birthYear = request.getBirthDate().getYear();
            if (birthYear < 1920 || birthYear > (currentYear - 5)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh không hợp lệ.");
            }
        }

        // === 6. Tự sinh mật khẩu ngẫu nhiên 12 ký tự, hash bcrypt ===
        String rawPassword = generateRandomPassword(12);
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // === 7. Quyết định status và activation ===
        boolean skipActivation = Boolean.TRUE.equals(request.getSkipActivation());
        UserStatus initialStatus = skipActivation ? UserStatus.ACTIVE : UserStatus.PENDING;

        // === 8. Build User entity ===
        User user = User.builder()
                .username(request.getUsername())
                .password(hashedPassword)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .studentCode(request.getStudentCode())
                .employeeCode(request.getEmployeeCode())
                .status(initialStatus)
                .roles(Set.of(role))
                .build();

        // === 9. Nếu KHÔNG skip activation → sinh activation token (32 byte hex, TTL 7 ngày) ===
        String activationToken = null;
        if (!skipActivation) {
            activationToken = generateActivationToken();
            user.setActivationToken(activationToken);
            user.setActivationTokenExpiry(LocalDateTime.now().plusDays(7));
        }

        // === 10. Lưu user ===
        User savedUser = userRepository.save(user);

        // === 11. Gán sinh viên vào lớp học (nếu có) ===
        if (classRoom != null) {
            classRoom.getStudents().add(savedUser);
            classRoomRepository.save(classRoom);
        }

        // === 12. Gửi email kích hoạt hoặc log password ===
        String activationMessage;
        String returnedPassword = null;

        if (skipActivation) {
            // Flow A2: Active ngay, trả password 1 lần cho Admin copy
            activationMessage = "Đã tạo user với trạng thái Active. Mật khẩu được hiển thị 1 lần duy nhất.";
            returnedPassword = rawPassword;
            log.info("UC08-A2: User [{}] tạo Active ngay, password hiển thị 1 lần.", savedUser.getUsername());
        } else {
            // Flow chính: Gửi email kích hoạt
            try {
                emailService.sendActivationEmail(savedUser.getEmail(), savedUser.getFullName(), activationToken);
                activationMessage = "Đã tạo user. Email kích hoạt đã được gửi.";
            } catch (Exception e) {
                // E3: SMTP thất bại → user vẫn lưu (status=Pending), Admin có thể gửi lại
                log.error("UC08-E3: Gửi email kích hoạt thất bại cho user [{}]: {}", savedUser.getUsername(), e.getMessage());
                activationMessage = "Đã tạo user nhưng gửi email kích hoạt thất bại. Vui lòng sử dụng nút 'Gửi lại email kích hoạt'.";
            }
        }

        // === 13. Build response ===
        return mapToCreateResponse(savedUser, returnedPassword, activationMessage);
    }

    /**
     * Kích hoạt tài khoản user qua activation token.
     */
    @Transactional
    public void activateUser(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token kích hoạt không hợp lệ hoặc không tồn tại."));

        if (user.getActivationTokenExpiry() == null || user.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token kích hoạt đã hết hạn. Vui lòng liên hệ Admin để gửi lại.");
        }

        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản đã được kích hoạt trước đó.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setActivationToken(null);
        user.setActivationTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Gửi lại email kích hoạt (khi SMTP thất bại lần đầu - E3).
     */
    @Transactional
    public void resendActivationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể gửi lại email kích hoạt cho user có trạng thái Pending.");
        }

        // Sinh token mới, reset TTL
        String newToken = generateActivationToken();
        user.setActivationToken(newToken);
        user.setActivationTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        emailService.sendActivationEmail(user.getEmail(), user.getFullName(), newToken);
    }

    // ======================== CẬP NHẬT USER (Admin CRUD) ========================
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User không tồn tại"
                ));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể cập nhật tài khoản đã bị xóa"
            );
        }
        // 1. Kiểm tra username
        if (userRepository.existsByUsernameAndIdNot(
                request.getUsername(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username đã được sử dụng"
            );
        }
        // 2. Kiểm tra email
        if (userRepository.existsByEmailAndIdNot(
                request.getEmail(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email đã được sử dụng"
            );
        }
        // 3. Kiểm tra studentCode
        if (request.getStudentCode() != null
                && !request.getStudentCode().isBlank()
                && userRepository.existsByStudentCodeAndIdNot(
                request.getStudentCode(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã sinh viên đã tồn tại"
            );
        }
        // 4. Kiểm tra employeeCode
        if (request.getEmployeeCode() != null
                && !request.getEmployeeCode().isBlank()
                && userRepository.existsByEmployeeCodeAndIdNot(
                request.getEmployeeCode(),
                id
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã nhân viên đã tồn tại"
            );
        }

        // 5. Validate ngày sinh
        validateBirthDate(request.getBirthDate());

        // 7. Cập nhật thông tin
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setBirthDate(request.getBirthDate());
        user.setGender(request.getGender());
        user.setStudentCode(request.getStudentCode());
        user.setEmployeeCode(request.getEmployeeCode());
        // Lưu trước để đảm bảo user tồn tại
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }
    // ======================== XÓA USER – SOFT DELETE  ========================

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        // Soft delete: đổi status thay vì xóa
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        // Thu hồi tất cả phiên đăng nhập
        refreshTokenRepository.deleteAllByUser(user);
    }

    // ======================== KHÓA/MỞ KHÓA USER ========================
    @Transactional
    public UserResponse lockUser(Long id, LockedRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể khóa tài khoản đã bị xóa");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản đã bị khóa");
        }

        user.setStatus(UserStatus.LOCKED);
        user.setLockedUntil(request.getLockUntil());
        User saveUser = userRepository.save(user);

        return mapToResponse(saveUser);
    }

    @Transactional
    public UserResponse unlockUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể mở khóa tài khoản đã bị xóa");
        }
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản hiện không bị khóa");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        User unlockedUser = userRepository.save(user);
        return mapToResponse(unlockedUser);
    }


    // ======================== ASSIGN/REVOKE ROLE (UC06) ========================


    @Transactional
    public UserResponse assignRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        // Không gán role cho user LOCKED/DELETED
        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản không hoạt động, không thể gán role");
        }

        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (newRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy role nào hợp lệ");
        }

        user.getRoles().addAll(newRoles);  // Thêm, không ghi đè
        return mapToResponse(userRepository.save(user));
    }


    @Transactional
    public UserResponse revokeRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        // BR-014: Mỗi user phải có ít nhất 1 role
        if (user.getRoles().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể thu hồi role cuối cùng. Mỗi user phải có ít nhất 1 role.");
        }

        Role roleToRemove = user.getRoles().stream().filter(r -> r.getId().equals(roleId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không có role này"));

        user.getRoles().remove(roleToRemove);
        return mapToResponse(userRepository.save(user));
    }


    // ======================== MAPPING ========================

    private UserResponse mapToResponse(User user) {
        List<String> roleCode = null;
        List<String> permissionCode = null;
        List<String> classCode = null;

        if (user.getRoles() != null) {
            roleCode = user.getRoles()
                    .stream()
                    .map(Role::getCode)
                    .collect(Collectors.toList());
        }
        if (user.getRoles() != null) {
            permissionCode = user.getRoles()
                    .stream()
                    .filter(r -> r.getPermissions() != null)
                    .flatMap(r -> r.getPermissions().stream())
                    .map(p -> p.getAction() + ":" + p.getFeature().getCode())
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (user.getClassRooms() != null) {
            classCode = user.getClassRooms()
                    .stream()
                    .map(ClassRoom::getCode)
                    .collect(Collectors.toList());
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .studentCode(user.getStudentCode())
                .employeeCode(user.getEmployeeCode())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .requirePasswordChange(user.getRequirePasswordChange())
                .createdAt(user.getCreatedAt())
                .roles(roleCode)
                .permissions(permissionCode)
                .classCodes(classCode)
                .build();
    }


    private UserResponseDto mapTo(User user) {
        String roleName = null;
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleName = user.getRoles().iterator().next().getCode();
        }
        String classCode = null;
        if (user.getClassRooms() != null && !user.getClassRooms().isEmpty()) {
            classCode = user.getClassRooms().stream().map(ClassRoom::getCode).findFirst().orElse(null);
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus())
                .classCodes(classCode)
                .roleNames(roleName)
                .build();
    }

    // ======================== HELPER METHODS ========================

    /**
     * Sinh mật khẩu ngẫu nhiên có đủ: uppercase, lowercase, digit, special char.
     */
    private String generateRandomPassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        // Đảm bảo có ít nhất 1 ký tự mỗi loại
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        // Điền phần còn lại ngẫu nhiên
        for (int i = 4; i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Xáo trộn để ký tự bắt buộc không luôn ở đầu
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    /**
     * Sinh activation token 32 byte hex (64 ký tự).
     */
    private String generateActivationToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Mapping User entity → CreateUserResponse (UC08).
     */
    private CreateUserResponse mapToCreateResponse(User user, String generatedPassword, String activationMessage) {
        List<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).collect(Collectors.toList())
                : Collections.emptyList();

        return CreateUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .studentCode(user.getStudentCode())
                .employeeCode(user.getEmployeeCode())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .roles(roleCodes)
                .generatedPassword(generatedPassword)
                .activationMessage(activationMessage)
                .build();
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return;
        }

        int currentYear = LocalDate.now().getYear();
        int birthYear = birthDate.getYear();

        if (birthYear < 1920 || birthYear > currentYear - 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày sinh không hợp lệ"
            );
        }
    }

}
