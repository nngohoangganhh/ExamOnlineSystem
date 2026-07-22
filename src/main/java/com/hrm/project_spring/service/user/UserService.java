package com.hrm.project_spring.service.user;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.*;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.*;
import com.hrm.project_spring.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClassRoomRepository classRoomRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ======================== LẤY DANH SÁCH USER ========================
    @Transactional
    public PageResponse<UserResponseDto> getAllUsers(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
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
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        return mapToResponse(user);
    }

    // ======================== TÌM KIẾM / LỌC USER (UC14) ========================
    @Transactional
    public PageResponse<UserResponseDto> searchUsers(UserSearchRequest search, int pageNo, int pageSize) {
        // UC14-E1: Validate keyword nếu có
        if (search.getKeyword() != null && !search.getKeyword().isBlank()
                && search.getKeyword().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }

        // Chuyển LocalDate → LocalDateTime để query
        LocalDateTime createdFrom = search.getCreatedFrom() != null
                ? search.getCreatedFrom().atStartOfDay() : null;
        LocalDateTime createdTo = search.getCreatedTo() != null
                ? search.getCreatedTo().atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        String keyword = (search.getKeyword() == null || search.getKeyword().isBlank())
                ? null : search.getKeyword().trim();

        Page<User> page = userRepository.searchUsers(
                keyword,
                search.getStatus(),
                search.getRoleId(),
                search.getClassId(),
                search.isIncludeDeleted(),
                createdFrom,
                createdTo,
                pageable
        );

        List<UserResponseDto> content = page.getContent().stream()
                .map(this::mapTo)
                .collect(Collectors.toList());

        return PageResponse.<UserResponseDto>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
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

        // === 2. Validate role tồn tại (UC08 Minor: chỉ lấy role active) ===
        List<Role> roles = roleRepository.findAllById(request.getRoleIds());

        if (roles.size() != request.getRoleIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có role không hợp lệ.");
        }

        // === 3. Nếu role = STUDENT → classId bắt buộc ===
        boolean isStudent = roles.stream()
                .anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getCode()));
        Set<ClassRoom> classRooms = new HashSet<>();
        if (isStudent) {
            if (request.getClassIds() == null || request.getClassIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "ClassId bắt buộc khi role là Student.");
            }
            List<ClassRoom> classrooms = classRoomRepository.findAllById(request.getClassIds());
            if (classrooms.size() != request.getClassIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có lớp học không hợp lệ.");
            }
            classRooms.addAll(classrooms);
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
            validateBirthDate(request.getBirthDate());
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
                .roles(new HashSet<>(roles))
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
        for (ClassRoom classRoom : classRooms) {
            classRoom.getStudents().add(savedUser);
            classRoomRepository.save(classRoom);
        }

        // === 12. Gửi email kích hoạt hoặc log password ===
        String activationMessage;
        String returnedPassword = null;

        if (skipActivation) {
            activationMessage = "Đã tạo user với trạng thái Active. Mật khẩu được hiển thị 1 lần duy nhất.";
            returnedPassword = rawPassword;
            log.info("UC08-A2: User [{}] tạo Active ngay, password hiển thị 1 lần.", savedUser.getUsername());
        } else {
            try {
                emailService.sendActivationEmail(savedUser.getEmail(), savedUser.getFullName(), activationToken);
                activationMessage = "Đã tạo user. Email kích hoạt đã được gửi.";
            } catch (Exception e) {
                log.error("UC08-E3: Gửi email kích hoạt thất bại cho user [{}]: {}", savedUser.getUsername(), e.getMessage());
                activationMessage = "Đã tạo user nhưng gửi email kích hoạt thất bại. Vui lòng sử dụng nút 'Gửi lại email kích hoạt'.";
            }
        }

        return mapToCreateResponse(savedUser, returnedPassword, activationMessage);
    }

    /**
     * Kích hoạt tài khoản user qua activation token.
     */
    @Transactional
    public void activateUser(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token kích hoạt không hợp lệ hoặc không tồn tại."));

        if (user.getActivationTokenExpiry() == null
                || user.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Token kích hoạt đã hết hạn. Vui lòng liên hệ Admin để gửi lại.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ có thể gửi lại email kích hoạt cho user có trạng thái Pending.");
        }

        String newToken = generateActivationToken();
        user.setActivationToken(newToken);
        user.setActivationTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        emailService.sendActivationEmail(user.getEmail(), user.getFullName(), newToken);
    }

    // ======================== CẬP NHẬT USER (UC09) ========================

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cập nhật tài khoản đã bị xóa");
        }

        // UC09-BR-019: KHÔNG cho phép đổi email và username
        // (username/email bị loại bỏ khỏi UpdateUserRequest)

        // Validate studentCode unique (trừ chính user đó)
        if (request.getStudentCode() != null
                && !request.getStudentCode().isBlank()
                && userRepository.existsByStudentCodeAndIdNot(request.getStudentCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }

        // Validate employeeCode unique (trừ chính user đó)
        if (request.getEmployeeCode() != null
                && !request.getEmployeeCode().isBlank()
                && userRepository.existsByEmployeeCodeAndIdNot(request.getEmployeeCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã nhân viên đã tồn tại");
        }

        // Validate ngày sinh
        validateBirthDate(request.getBirthDate());

        // Cập nhật thông tin (KHÔNG set username/email)
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setBirthDate(request.getBirthDate());
        user.setGender(request.getGender());
        user.setStudentCode(request.getStudentCode());
        user.setEmployeeCode(request.getEmployeeCode());

        // UC09: Cập nhật lớp cho Student nếu có classIds
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            List<ClassRoom> newClassRooms = classRoomRepository.findAllById(request.getClassIds());
            if (newClassRooms.size() != request.getClassIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có lớp học không hợp lệ.");
            }
            // Gỡ user khỏi tất cả lớp cũ rồi gán vào lớp mới
            if (user.getClassRooms() != null) {
                for (ClassRoom oldClass : user.getClassRooms()) {
                    oldClass.getStudents().remove(user);
                    classRoomRepository.save(oldClass);
                }
            }
            for (ClassRoom newClass : newClassRooms) {
                newClass.getStudents().add(user);
                classRoomRepository.save(newClass);
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    // ======================== XÓA USER – SOFT DELETE (UC11) ========================

    @Transactional
    public void deleteUser(Long id, DeleteUserRequest request, String currentUsername) {
        // UC11-E5: Admin không tự xóa mình
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác thực được admin"));
        if (currentUser.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Không thể xóa tài khoản của chính mình");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản đã bị xóa trước đó");
        }

        // UC11-E2: confirmName phải khớp username
        if (!user.getUsername().equals(request.getConfirmName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tên xác nhận không khớp với tên đăng nhập của user cần xóa");
        }

        // UC11-E1: Không có lượt thi đang diễn ra
        if (examAttemptRepository.existsByUserIdAndSubmitTimeIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa user đang có bài thi chưa nộp");
        }

        // UC11-BR-024: Thêm hậu tố _deleted_{timestamp} vào email/username
        String timestamp = String.valueOf(System.currentTimeMillis());
        user.setUsername(user.getUsername() + "_deleted_" + timestamp);
        user.setEmail(user.getEmail() + "_deleted_" + timestamp);

        // UC11: Set deletedAt và status
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);

        // Thu hồi tất cả phiên đăng nhập
        refreshTokenRepository.deleteAllByUser(user);

        log.info("UC11: Admin [{}] xóa user [{}] với lý do: {}", currentUsername, id, request.getReason());

        // Gửi email thông báo cho user
        try {
            emailService.sendAccountDeletedEmail(user.getEmail(), user.getFullName(), request.getReason());
        } catch (Exception e) {
            log.warn("UC11: Gửi email thông báo xóa tài khoản thất bại cho [{}]: {}", id, e.getMessage());
        }
    }

    /**
     * UC11-A1: Restore user trong 30 ngày sau khi soft-delete.
     */
    @Transactional
    public UserResponse restoreUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() != UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User chưa bị xóa");
        }

        // Kiểm tra 30 ngày
        if (user.getDeletedAt() == null
                || user.getDeletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Không thể khôi phục: đã quá 30 ngày kể từ khi xóa");
        }

        // Gỡ hậu tố _deleted_timestamp khỏi username/email
        String username = user.getUsername();
        String email = user.getEmail();
        int usernameDelIdx = username.lastIndexOf("_deleted_");
        int emailDelIdx = email.lastIndexOf("_deleted_");
        if (usernameDelIdx > 0) user.setUsername(username.substring(0, usernameDelIdx));
        if (emailDelIdx > 0) user.setEmail(email.substring(0, emailDelIdx));

        user.setStatus(UserStatus.PENDING);
        user.setDeletedAt(null);
        user.setRequirePasswordChange(true);
        User restored = userRepository.save(user);

        log.info("UC11-A1: Khôi phục user [{}]", id);
        return mapToResponse(restored);
    }

    // ======================== KHÓA/MỞ KHÓA USER (UC10) ========================

    @Transactional
    public UserResponse lockUser(Long id, LockedRequest request, String currentUsername) {
        // UC10-E1: Admin không tự khóa mình
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác thực được admin"));
        if (currentUser.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Không thể khóa tài khoản của chính mình");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể khóa tài khoản đã bị xóa");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản đã bị khóa");
        }

        // UC10: Validate lockUntil nếu có
        if (request.getLockUntil() != null) {
            LocalDateTime minLockUntil = LocalDateTime.now().plusHours(1);
            LocalDateTime maxLockUntil = LocalDateTime.now().plusYears(5);
            if (request.getLockUntil().isBefore(minLockUntil)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thời hạn khóa phải sau thời điểm hiện tại ít nhất 1 giờ");
            }
            if (request.getLockUntil().isAfter(maxLockUntil)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thời hạn khóa không được quá 5 năm");
            }
        }

        // UC10-BR-021: Lưu lý do khóa
        user.setStatus(UserStatus.LOCKED);
        user.setLockReason(request.getReason());
        user.setLockedUntil(request.getLockUntil());
        User savedUser = userRepository.save(user);

        // UC10: Thu hồi toàn bộ refresh token
        refreshTokenRepository.deleteAllByUser(user);

        log.info("UC10: Admin [{}] khóa user [{}] với lý do: {}", currentUsername, id, request.getReason());

        // Gửi email thông báo cho user bị khóa
        try {
            emailService.sendAccountLockedEmail(user.getEmail(), user.getFullName(), request.getReason(), request.getLockUntil());
        } catch (Exception e) {
            log.warn("UC10: Gửi email thông báo khóa thất bại cho user [{}]: {}", id, e.getMessage());
        }

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể mở khóa tài khoản đã bị xóa");
        }
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản hiện không bị khóa");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLockReason(null);

        // UC10-A1: Yêu cầu đổi mật khẩu lần đăng nhập tiếp
        user.setRequirePasswordChange(true);

        User unlockedUser = userRepository.save(user);
        return mapToResponse(unlockedUser);
    }

    // ======================== ASSIGN/REVOKE ROLE ========================

    @Transactional
    public UserResponse assignRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản không hoạt động, không thể gán role");
        }

        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (newRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy role nào hợp lệ");
        }
        user.getRoles().addAll(newRoles);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse revokeRole(Long userId, List<Long> roleId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        List<Role> roleToRemove = user.getRoles()
                .stream()
                .filter(role -> roleId.contains(role.getId()))
                .toList();

        if (roleToRemove.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User không có role cần thu hồi");
        }

        if (user.getRoles().size() - roleToRemove.size() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể thu hồi role cuối cùng");
        }
        user.getRoles().removeAll(roleToRemove);
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

        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

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
        if (birthDate == null) return;
        int currentYear = LocalDate.now().getYear();
        int birthYear = birthDate.getYear();
        if (birthYear < 1920 || birthYear > currentYear - 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh không hợp lệ");
        }
    }
}
