package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC42–UC44, UC47: Báo cáo kết quả thi.
 */
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    // ─── UC42: Kết quả thí sinh theo bài thi ─────────────────────────────
    @PreAuthorize("hasAuthority('RESULT:VIEW')")
    @GetMapping("/tests/{testId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTestResults(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .code(200)
                .message("Lấy kết quả bài thi thành công")
                .data(resultService.getTestResults(testId))
                .build());
    }

    // ─── UC43: Thống kê bài thi ───────────────────────────────────────────
    @PreAuthorize("hasAuthority('RESULT:VIEW')")
    @GetMapping("/tests/{testId}/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTestStatistics(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .code(200)
                .message("Lấy thống kê bài thi thành công")
                .data(resultService.getTestStatistics(testId))
                .build());
    }

    // ─── UC44: Phân tích câu hỏi ──────────────────────────────────────────
    @PreAuthorize("hasAuthority('RESULT:VIEW')")
    @GetMapping("/tests/{testId}/question-analysis")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuestionAnalysis(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .code(200)
                .message("Phân tích câu hỏi thành công")
                .data(resultService.getQuestionAnalysis(testId))
                .build());
    }

    // ─── UC47: Lịch sử làm bài của thí sinh (đang đăng nhập) ──────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyHistory() {
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .code(200)
                .message("Lấy lịch sử làm bài thành công")
                .data(resultService.getMyHistory())
                .build());
    }
}