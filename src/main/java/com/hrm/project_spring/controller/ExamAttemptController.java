package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.result.AttemptDetailResponse;
import com.hrm.project_spring.dto.result.AttemptSummaryResponse;
import com.hrm.project_spring.service.ExamAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.hrm.project_spring.dto.result.AttemptSubmitRequest;
import com.hrm.project_spring.dto.exam.ExamAttemptStart;
import com.hrm.project_spring.dto.exam.ExamAttemptSubmit;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class ExamAttemptController {

    private final ExamAttemptService attemptService;

    // ======================== THI ========================

    @PreAuthorize("hasAuthority('EXAM:START')")
    @PostMapping("/start/test/{testId}")
    public ResponseEntity<ApiResponse<ExamAttemptStart>> startAttempt(
            @PathVariable Long testId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamAttemptStart>builder()
                .success(true)
                .code(200)
                .message("Bắt đầu bài thi thành công")
                .data(attemptService.startAttempt(testId, authentication.getName()))
                .build());
    }

    @PreAuthorize("hasAuthority('EXAM:SUBMIT')")
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ApiResponse<ExamAttemptSubmit>> submitAttempt(
            @PathVariable Long attemptId,
            @Valid @RequestBody AttemptSubmitRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<ExamAttemptSubmit>builder()
                .success(true)
                .code(200)
                .message("Nộp bài thành công")
                .data(attemptService.submitAttempt(attemptId, authentication.getName(), request))
                .build());
    }

    // ======================== NEW: LỊCH SỬ THI (STUDENT) ========================

    /**
     * Học sinh xem lịch sử thi của chính mình
     * GET /api/attempts/my?pageNo=0&pageSize=10
     */
    @PreAuthorize("hasAuthority('EXAM:START')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<AttemptSummaryResponse>>> getMyAttempts(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttemptSummaryResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy lịch sử thi thành công")
                .data(attemptService.getMyAttempts(authentication.getName(), pageNo, pageSize))
                .build());
    }

    /**
     * Học sinh xem chi tiết kết quả 1 lần thi (bao gồm từng câu đúng/sai)
     * GET /api/attempts/{attemptId}
     */
    @PreAuthorize("hasAuthority('EXAM:START')")
    @GetMapping("/{attemptId}")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> getAttemptDetail(
            @PathVariable Long attemptId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<AttemptDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết bài thi thành công")
                .data(attemptService.getAttemptDetail(attemptId, authentication.getName()))
                .build());
    }

    // ======================== NEW: ADMIN XEM KẾT QUẢ ========================

    /**
     * Admin xem tất cả attempts của 1 test
     * GET /api/attempts/test/{testId}?pageNo=0&pageSize=10
     */
    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping("/test/{testId}")
    public ResponseEntity<ApiResponse<PageResponse<AttemptSummaryResponse>>> getAttemptsByTest(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttemptSummaryResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách bài thi theo test thành công")
                .data(attemptService.getAttemptsByTest(testId, pageNo, pageSize))
                .build());
    }

    /**
     * Admin xem tất cả attempts của 1 exam
     * GET /api/attempts/exam/{examId}?pageNo=0&pageSize=10
     */
    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<PageResponse<AttemptSummaryResponse>>> getAttemptsByExam(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttemptSummaryResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách bài thi theo kỳ thi thành công")
                .data(attemptService.getAttemptsByExam(examId, pageNo, pageSize))
                .build());
    }

    /**
     * Admin xem chi tiết bất kỳ attempt
     * GET /api/attempts/{attemptId}/admin
     */
    @PreAuthorize("hasAuthority('EXAM:READ')")
    @GetMapping("/{attemptId}/admin")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> getAttemptDetailAdmin(
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(ApiResponse.<AttemptDetailResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết bài thi thành công")
                .data(attemptService.getAttemptDetailAdmin(attemptId))
                .build());
    }
}
