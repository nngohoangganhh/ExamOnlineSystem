package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.service.ParticipationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho UC33–UC41: Participation flow.
 * Base path: /api/participation
 */
@RestController
@RequestMapping("/api/participation")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    // UC33: Danh sách bài thi có thể tham gia
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @GetMapping("/available-tests")
    public ResponseEntity<ApiResponse<List<Test>>> getAvailableTests() {
        return ResponseEntity.ok(ApiResponse.<List<Test>>builder()
                .success(true).code(200)
                .message("Danh sách bài thi")
                .data(participationService.getAvailableTests())
                .build());
    }

    // UC34: Vào thi (tạo Attempt)
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @PostMapping("/tests/{testId}/start")
    public ResponseEntity<ApiResponse<Attempt>> startAttempt(@PathVariable Long testId,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.<Attempt>builder()
                .success(true).code(201)
                .message("Bắt đầu làm bài thành công")
                .data(participationService.startAttempt(testId, request))
                .build());
    }

    // UC35/UC36: Lưu câu trả lời / auto-save
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @PostMapping("/attempts/{attemptId}/answers/{questionId}")
    public ResponseEntity<ApiResponse<AttemptAnswer>> saveAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody Map<String, String> body) {
        String answerData = body.getOrDefault("answerData", "");
        return ResponseEntity.ok(ApiResponse.<AttemptAnswer>builder()
                .success(true).code(200)
                .message("Lưu câu trả lời thành công")
                .data(participationService.saveAnswer(attemptId, questionId, answerData))
                .build());
    }

    // UC38: Đánh dấu xem lại
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @PatchMapping("/attempts/{attemptId}/answers/{questionId}/mark")
    public ResponseEntity<ApiResponse<Void>> toggleMarkForReview(
            @PathVariable Long attemptId,
            @PathVariable Long questionId) {
        participationService.toggleMarkForReview(attemptId, questionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).code(200)
                .message("Đã cập nhật đánh dấu xem lại")
                .data(null)
                .build());
    }

    // UC39: Nộp bài
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<Attempt>> submit(@PathVariable Long attemptId,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.<Attempt>builder()
                .success(true).code(200)
                .message("Nộp bài thành công")
                .data(participationService.submit(attemptId, request))
                .build());
    }

    // UC41: Xem lại bài thi
    @PreAuthorize("hasAuthority('EXAM:TAKE')")
    @GetMapping("/attempts/{attemptId}/review")
    public ResponseEntity<ApiResponse<List<AttemptAnswer>>> reviewAttempt(
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(ApiResponse.<List<AttemptAnswer>>builder()
                .success(true).code(200)
                .message("Kết quả bài làm")
                .data(participationService.reviewAttempt(attemptId))
                .build());
    }
}
