package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.feature.FeatureRequest;
import com.hrm.project_spring.dto.feature.FeatureResponse;
import com.hrm.project_spring.service.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @PreAuthorize("hasAuthority('FEATURE:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeatureResponse>>> getAllFeatures(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<FeatureResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách feature thành công")
                .data(featureService.getAllFeatures(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('FEATURE:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeatureResponse>> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<FeatureResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiết feature")
                .data(featureService.getFeatureById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('FEATURE:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<FeatureResponse>> createFeature(@RequestBody @Valid FeatureRequest request) {
        return ResponseEntity.ok(ApiResponse.<FeatureResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo feature thành công")
                .data(featureService.createFeature(request))
                .build());
    }

    @PreAuthorize("hasAuthority('FEATURE:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeatureResponse>> updateFeature(
            @PathVariable Long id,
            @RequestBody @Valid FeatureRequest request) {
        return ResponseEntity.ok(ApiResponse.<FeatureResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật feature thành công")
                .data(featureService.updateFeature(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('FEATURE:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(@PathVariable Long id) {
        featureService.deleteFeature(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code (200)
                .message("Xóa feature thành công")
                .data(null)
                .build());
    }
}
