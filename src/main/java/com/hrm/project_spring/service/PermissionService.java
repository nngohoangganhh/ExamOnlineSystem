package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.permission.PermissionRequest;
import com.hrm.project_spring.dto.permission.PermissionResponse;
import com.hrm.project_spring.entity.Feature;
import com.hrm.project_spring.entity.Permission;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.repository.FeatureRepository;
import com.hrm.project_spring.repository.PermissionRepository;
import com.hrm.project_spring.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final FeatureRepository featureRepository;
    private final RoleRepository roleRepository;
//1
    public PageResponse<PermissionResponse> getAllPermissions(int pageNo, int pageSize) {
        Page<Permission> page = permissionRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<PermissionResponse> data = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<PermissionResponse>builder()
                .content(data)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
//2
    public PermissionResponse getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        return mapToResponse(permission);
    }
//3
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permission code already exists: " + request.getCode());
        }
        Feature feature = featureRepository.findById(request.getFeatureId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));

        Permission permission = Permission.builder()
                .code(request.getCode().toUpperCase())
                .action(request.getAction().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .feature(feature)
                .build();
        return mapToResponse(permissionRepository.save(permission));
    }
//4
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        Feature feature = featureRepository.findById(request.getFeatureId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));

        permission.setCode(request.getCode().toUpperCase());
        permission.setAction(request.getAction().toUpperCase());
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setFeature(feature);
        return mapToResponse(permissionRepository.save(permission));
    }
//5
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        permissionRepository.delete(permission);
    }


    //1
    @Transactional
    public void assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.getPermissions().addAll(new HashSet<>(permissions));
        roleRepository.save(role);
    }


    //2
    @Transactional
    public void removePermissionsFromRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        role.getPermissions().removeIf(p -> permissionIds.contains(p.getId()));
        roleRepository.save(role);
    }

    public PermissionResponse mapToResponse(Permission permission) {
        String featureName = null;
        String featureCode = null;
        Long featureId = null;
        if (permission.getFeature() != null) {
            featureName = permission.getFeature().getName();
            featureCode = permission.getFeature().getCode();
            featureId = permission.getFeature().getId();
        }
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .action(permission.getAction())
                .name(permission.getName())
                .description(permission.getDescription())
                .featureId(featureId)
                .featureName(featureName)
                .featureCode(featureCode)
                .build();
    }
}
