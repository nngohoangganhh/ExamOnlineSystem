package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.permission.PermissionResponse;
import com.hrm.project_spring.dto.role.RoleRequest;
import com.hrm.project_spring.dto.role.RoleResponse;
import com.hrm.project_spring.entity.Permission;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.enums.RoleStatus;
import com.hrm.project_spring.repository.PermissionRepository;
import com.hrm.project_spring.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public Object getAllRoles(Integer pageNo, Integer pageSize) {
        // Chỉ truyền một trong hai param thì không hợp lệ
        if (pageNo == null ^ pageSize == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phải truyền đồng thời pageNo và pageSize"
            );
        }
        // Có truyền param thì kiểm tra giá trị
        if (pageNo != null) {
            if (pageNo < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "pageNo không được nhỏ hơn 0"
                );
            }
            if (pageSize <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "pageSize phải lớn hơn 0"
                );
            }
        }
        // Query số user của từng role
        List<Object[]> rows = roleRepository.countUsersByRole();

        Map<Long, Integer> userCountMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // Không truyền param: lấy toàn bộ role
        if (pageNo == null && pageSize == null) {

            return roleRepository.findAll()
                    .stream()
                    .map(role -> mapToResponse(
                            role,
                            userCountMap.getOrDefault(role.getId(), 0)
                    ))
                    .toList();
        }
        // Có truyền đủ param: lấy dữ liệu phân trang
        Page<Role> page = roleRepository.findAll(
                PageRequest.of(pageNo, pageSize)
        );

        List<RoleResponse> data = page.getContent()
                .stream()
                .map(role -> mapToResponse(role, userCountMap.getOrDefault(role.getId(), 0)))
                .toList();
        return PageResponse.<RoleResponse>builder()
                .content(data)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role không tồn tại"));
        return mapToResponse(role);
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {

        String codeUpper = request.getCode().toUpperCase();

        // Kiểm tra trùng code
        if (roleRepository.existsByCode(codeUpper)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã role '" + codeUpper + "' đã tồn tại");
        }

        // Kiểm tra trùng name
        if (roleRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên role '" + request.getName() + "' đã tồn tại");
        }

        Role role = Role.builder()
                .code(codeUpper)
                .name(request.getName())
                .description(request.getDescription())
                .isSystem(false)            // Role mới tạo luôn KHÔNG phải system
                .status(RoleStatus.ACTIVE)  // Mặc định ACTIVE
                .build();

        // Gán permissions nếu có
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role không tồn tại"));

        String codeUpper = request.getCode().toUpperCase();

        if (role.isSystem()) {
            // BR-011: Role hệ thống chỉ cho sửa description + permissions
            // Không cho đổi code hoặc name
            if (!role.getCode().equals(codeUpper) ||
                    !role.getName().equals(request.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Không thể đổi tên hoặc mã role hệ thống: " + role.getCode());
            }
        } else {
            // Role thường: cho sửa code/name, nhưng kiểm tra trùng với role khác
            if (!role.getCode().equals(codeUpper) &&
                    roleRepository.existsByCode(codeUpper)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Mã role '" + codeUpper + "' đã tồn tại");
            }
            if (!role.getName().equals(request.getName()) &&
                    roleRepository.existsByName(request.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tên role '" + request.getName() + "' đã tồn tại");
            }
            role.setCode(codeUpper);
            role.setName(request.getName());
        }

        // Description cho sửa bất kể system hay không
        role.setDescription(request.getDescription());

        // Permissions cho sửa bất kể system hay không
        // (Admin cần cập nhật quyền cho role hệ thống)
        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        return mapToResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role không tồn tại"));

        // BR-011: Không cho xóa role hệ thống
        if (role.isSystem()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không thể xóa role hệ thống: " + role.getCode());
        }

        // Không cho xóa role đang được gán cho user
        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Role đang được gán cho " + role.getUsers().size() +
                            " người dùng. Vui lòng chuyển họ sang role khác trước.");
        }

        roleRepository.delete(role);
    }

    public RoleResponse mapToResponse(Role role, int totalUser) {
        List<PermissionResponse> permissions = null;
        if (role.getPermissions() != null) {
            permissions = role.getPermissions().stream()
                    .map(p -> PermissionResponse.builder()
                            .id(p.getId())
                            .code(p.getCode())
                            .action(p.getAction())
                            .name(p.getName())
                            .description(p.getDescription())
                            .featureId(p.getFeature() != null ? p.getFeature().getId() : null)
                            .featureName(p.getFeature() != null ? p.getFeature().getName() : null)
                            .featureCode(p.getFeature() != null ? p.getFeature().getCode() : null)
                            .build())
                    .collect(Collectors.toList());
        }
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.isSystem())
                .status(role.getStatus() != null ? role.getStatus().name() : "ACTIVE")
                .totalUser(totalUser)
                .permissions(permissions)
                .build();
    }

    public RoleResponse mapToResponse(Role role) {
        int totalUser = (role.getUsers() != null) ? role.getUsers().size() : 0;
        return mapToResponse(role, totalUser);
    }
}
