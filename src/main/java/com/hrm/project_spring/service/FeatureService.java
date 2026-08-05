package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.feature.FeatureRequest;
import com.hrm.project_spring.dto.feature.FeatureResponse;
import com.hrm.project_spring.dto.permission.PermissionResponse;
import com.hrm.project_spring.entity.Feature;
import com.hrm.project_spring.repository.FeatureRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;
//1
    public PageResponse<FeatureResponse> getAllFeatures(int pageNo, int pageSize) {
        Page<Feature> page = featureRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<FeatureResponse> data = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<FeatureResponse>builder()
                .content(data)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
//2
    public FeatureResponse getFeatureById(Long id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        return mapToResponse(feature);
    }
//3
    public FeatureResponse createFeature(FeatureRequest request) {
        if (featureRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feature code already exists: " + request.getCode());
        }
        Feature feature = Feature.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return mapToResponse(featureRepository.save(feature));
    }
//4
    @Transactional
    public FeatureResponse updateFeature(Long id, FeatureRequest request) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        feature.setCode(request.getCode().toUpperCase());
        feature.setName(request.getName());
        feature.setDescription(request.getDescription());
        return mapToResponse(featureRepository.save(feature));
    }
//5
    public void deleteFeature(Long id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        featureRepository.delete(feature);
    }

    public FeatureResponse mapToResponse(Feature feature) {
        List<PermissionResponse> permissions = null;
        if (feature.getPermissions() != null) {
            permissions = feature.getPermissions().stream()
                    .map(p -> PermissionResponse.builder()
                            .id(p.getId())
                            .code(p.getCode())
                            .action(p.getAction())
                            .name(p.getName())
                            .description(p.getDescription())
                            .featureId(feature.getId())
                            .featureName(feature.getName())
                            .featureCode(feature.getCode())
                            .build())
                    .collect(Collectors.toList());
        }
        return FeatureResponse.builder()
                .id(feature.getId())
                .code(feature.getCode())
                .name(feature.getName())
                .description(feature.getDescription())
                .permissions(permissions)
                .build();
    }
}
