package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.exam.ExamRequest;
import com.hrm.project_spring.dto.student.StudentResponse;
import com.hrm.project_spring.dto.student.AssignStudentsRequest;
import com.hrm.project_spring.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public ResponseEntity <ApiResponse<ExamDetailResponse>> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(200)
                        .message("tìm kỳ thi theo id thành công")
                        .data(examService.getExamById(id))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('EXAM:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<ExamDetailResponse>> create(@RequestBody @Valid ExamRequest request) {
           return ResponseEntity.ok(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(200)
                        .message(" Tạo kỳ thi thành công")
                        .data(examService.create(request))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('EXAM:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid ExamRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .code(200)
                        .message("tìm kỳ thi theo id thành công")
                        .data(examService.update(id,request))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('EXAM:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa thành công")
                .data(null)
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
