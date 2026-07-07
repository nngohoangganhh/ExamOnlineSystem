package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.permission.PermissionResponse;
import com.hrm.project_spring.dto.role.RoleRequest;
import com.hrm.project_spring.dto.role.RoleResponse;
import com.hrm.project_spring.entity.Permission;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.repository.PermissionRepository;
import com.hrm.project_spring.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public PageResponse<RoleResponse> getAllRoles(int pageNo, int pageSize) {
        Page<Role> page = roleRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<RoleResponse> data = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<RoleResponse>builder()
                .content(data)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return mapToResponse(role);
    }

    public RoleResponse createRole(RoleRequest request) {

        Role role = Role.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        return mapToResponse(roleRepository.save(role));
    }

    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        role.setCode(request.getCode().toUpperCase());
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        return mapToResponse(roleRepository.save(role));
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        roleRepository.delete(role);
    }

    public RoleResponse mapToResponse(Role role) {
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
                .permissions(permissions)
                .build();
    }
}
