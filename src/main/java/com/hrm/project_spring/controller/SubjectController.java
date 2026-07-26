package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.subject.SubjectRequest;
import com.hrm.project_spring.dto.subject.SubjectResponse;
import com.hrm.project_spring.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PreAuthorize("hasAuthority('SUBJECT:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.<SubjectResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo môn học thành công")
                .data(subjectService.createSubject(request))
                .build());
    }

    @PreAuthorize("hasAuthority('SUBJECT:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SubjectResponse>>> getAllSubjects(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<SubjectResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách môn học thành công")
                .data(subjectService.getAllSubjects(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('SUBJECT:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<SubjectResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết môn học thành công")
                .data(subjectService.getSubjectById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('SUBJECT:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.<SubjectResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật môn học thành công")
                .data(subjectService.updateSubject(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('SUBJECT:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa môn học thành công")
                .build());
    }
}
