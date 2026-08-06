package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.enrollment.EnrollmentResponse;
import com.hrm.project_spring.entity.Enrollment;
import com.hrm.project_spring.service.EnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests/{testId}/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<Integer>> enrollUsers(
            @PathVariable Long testId,
            @RequestBody List<Long> userIds,
            HttpServletRequest request) {
        int count = enrollmentService.enrollUsers(testId, userIds, request);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .success(true)
                .code(200)
                .message("Gán " + count + " thí sinh vào bài thi thành công")
                .data(count)
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/class/{classRoomId}")
    public ResponseEntity<ApiResponse<Integer>> enrollClassRoom(
            @PathVariable Long testId,
            @PathVariable Long classRoomId,
            HttpServletRequest request) {
        int count = enrollmentService.enrollClassRoom(testId, classRoomId, request);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .success(true)
                .code(200)
                .message("Gán " + count + " thí sinh trong lớp vào bài thi thành công")
                .data(count)
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> unenroll(
            @PathVariable Long testId,
            @PathVariable Long userId,
            HttpServletRequest request) {
        enrollmentService.unenroll(testId, userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Hủy gán thí sinh khỏi bài thi thành công")
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getEnrollments(@PathVariable Long testId) {
        List<EnrollmentResponse> enrollments = enrollmentService.getEnrollments(testId);
        return ResponseEntity.ok(ApiResponse.<List<EnrollmentResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách thí sinh bài thi thành công")
                .data(enrollments)
                .build());
    }
}
