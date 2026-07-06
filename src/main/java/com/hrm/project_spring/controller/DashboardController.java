package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.dashboard.AdminDashboardResponse;
import com.hrm.project_spring.dto.dashboard.StudentDashboardResponse;
import com.hrm.project_spring.dto.exam.MyExamResponse;
import com.hrm.project_spring.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Admin xem thống kê tổng quan hệ thống
     * GET /api/dashboard/admin
     */
    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy thống kê admin thành công")
                .data(dashboardService.getAdminDashboard())
                .build());
    }

    /**
     * Học sinh xem thống kê cá nhân
     * GET /api/dashboard/student
     */
    @PreAuthorize("hasAuthority('EXAM:START')")
    @GetMapping("/student")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getStudentDashboard() {
        return ResponseEntity.ok(ApiResponse.<StudentDashboardResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy thống kê học sinh thành công")
                .data(dashboardService.getStudentDashboard())
                .build());
    }

    /**
     * Học sinh xem danh sách kỳ thi được gán
     * GET /api/dashboard/my-exams
     */
    @PreAuthorize("hasAuthority('EXAM:START')")
    @GetMapping("/my-exams")
    public ResponseEntity<ApiResponse<List<MyExamResponse>>> getMyExams() {
        return ResponseEntity.ok(ApiResponse.<List<MyExamResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách kỳ thi thành công")
                .data(dashboardService.getMyExams())
                .build());
    }
}
