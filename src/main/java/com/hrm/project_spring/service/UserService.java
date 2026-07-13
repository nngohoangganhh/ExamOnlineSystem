package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.LockedRequest;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.dto.user.UserResponseDto;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.RefreshTokenRepository;
import com.hrm.project_spring.repository.RoleRepository;
import com.hrm.project_spring.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // ======================== LẤY DANH SÁCH USER ========================

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

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        return mapToResponse(user);
    }

    // ======================== TẠO USER========================

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã bị trùng");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã bị trùng");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .status(UserStatus.ACTIVE).build();

        // BR-014: Mỗi user phải có ít nhất 1 role
        Set<Role> roles;
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
        } else {
            // Gán STUDENT mặc định nếu không chỉ định role
            Role defaultRole = roleRepository.findByCode("STUDENT").orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình: thiếu role STUDENT trong hệ thống"));
            roles = Set.of(defaultRole);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    // ======================== CẬP NHẬT USER (Admin CRUD) ========================

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã bị trùng");
        }
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã bị trùng");
        }

        user.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

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

        if (user.getRoles() != null) {
            roleCode = user.getRoles().stream().map(Role::getCode).collect(Collectors.toList());
        }
        if (user.getRoles() != null) {
            permissionCode = user.getRoles().stream().filter(r -> r.getPermissions() != null).flatMap(r -> r.getPermissions().stream()).map(p -> p.getAction() + ":" + p.getFeature().getCode()).distinct().collect(Collectors.toList());
        }
        return UserResponse
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .lastLoginAt(user.getLastLoginAt())
                .requirePasswordChange(user.getRequirePasswordChange())
                .status(user.getStatus()).createdAt(user.getCreatedAt())
                .roles(roleCode)
                .permissions(permissionCode)
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
}
