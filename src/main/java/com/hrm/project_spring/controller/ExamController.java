package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.exam.ExamRequest;
import com.hrm.project_spring.dto.student.AssignStudentsRequest;
import com.hrm.project_spring.dto.student.StudentResponse;
import com.hrm.project_spring.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/exams")
@RestController
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    // ======================== CRUD ========================
    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExamListResponse>>> getAllExam(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<ExamListResponse>>builder()
                        .success(true)
                        .code(200)
                        .message("lấy danh sách thành công")
                        .data(examService.getAllExam(pageNo, pageSize))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(200)
                        .message("tìm kỳ thi theo id thành công")
                        .data(examService.getExamById(id))
                        .build()
        );
    }

    /**
     * UC25: Tạo kỳ thi. Trả 201 CREATED khi thành công.
     */
    @PreAuthorize("hasAuthority('EXAM:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<ExamDetailResponse>> create(
            @RequestBody @Valid ExamRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo kỳ thi thành công")
                        .data(examService.create(request, httpRequest))
                        .build()
        );
    }

    /**
     * UC26: Cập nhật kỳ thi (partial update — chỉ gửi field cần sửa).
     * A1: Nếu exam đang PUBLISHED, cần thêm confirm=true trong body.
     * E2: Không được sửa code khi đã PUBLISHED.
     */
    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> update(
            @PathVariable Long id,
            @RequestBody ExamRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Cập nhật kỳ thi thành công")
                        .data(examService.update(id, request, httpRequest))
                        .build()
        );
    }

    /**
     * UC26: Xóa kỳ thi (soft delete).
     * E1: Từ chối nếu có attempt completed (service throw 409).
     * Response trả deletedAt để FE biết không cần gọi thêm GET.
     */
    @PreAuthorize("hasAuthority('EXAM:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> deleteExam(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        ExamDetailResponse deleted = examService.deleteExam(id, httpRequest);
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Xóa kỳ thi thành công")
                .data(deleted)
                .build());
    }

    /**
     * UC25 A1: Publish kỳ thi (status = PUBLISHED).
     */
    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> publish(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Publish kỳ thi thành công")
                .data(examService.publish(id, httpRequest))
                .build());
    }

    /**
     * UC26 / BR-043: Archive kỳ thi.
     */
    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> archive(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Lưu trữ kỳ thi thành công")
                .data(examService.archive(id, httpRequest))
                .build());
    }

    // ======================== STUDENTS ========================

    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping("/{examId}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudentsByExamId(@PathVariable Long examId) {
        return ResponseEntity.ok(
                ApiResponse.<List<StudentResponse>>builder()
                        .success(true)
                        .code(200)
                        .message("lấy danh sách học sinh theo id kỳ thi thành công")
                        // Fix: Wrap Set<StudentResponse> into List to avoid ClassCastException
                        .data(new ArrayList<>(examService.getStudentsByExamId(examId)))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PostMapping("/{examId}/students")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> assignStudents(
            @PathVariable Long examId,
            @RequestBody @Valid AssignStudentsRequest request) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Gán học sinh thành công")
                .data(examService.assignStudentsToExam(examId, request.getStudentIds()))
                .build());
    }

    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @DeleteMapping("/{examId}/students")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> removeStudents(
            @PathVariable Long examId,
            @RequestBody @Valid AssignStudentsRequest request) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Xóa học sinh thành công")
                .data(examService.removeStudentsFromExam(examId, request.getStudentIds()))
                .build());
    }

    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PostMapping("/{examId}/classes/{classId}")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> assignClass(
            @PathVariable Long examId,
            @PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Gán lớp học vào kỳ thi thành công")
                .data(examService.assignClassToExam(examId, classId))
                .build());
    }
}
