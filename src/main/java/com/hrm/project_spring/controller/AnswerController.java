package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.answer.AnswerRequest;
import com.hrm.project_spring.dto.answer.AnswerResponse;
import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController {
    private final AnswerService answerService;
    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> getAnswersByQuestion(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "false") boolean isExamTime) {
        boolean includeIsCorrect = !isExamTime; 
        return ResponseEntity.ok(
                ApiResponse.<List<AnswerResponse>>builder()
                        .success(true)
                        .code(200)
                        .message("Lấy danh sách thành công")
                        .data(answerService.getAnswersByQuestionId(questionId, includeIsCorrect))
                        .build()
        );
    }
    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AnswerResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo thành công")
                        .data(answerService.addAnswerToQuestion(questionId, request))
                        .build());
    }
    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> createBulkAnswers(
            @PathVariable Long questionId,
            @Valid @RequestBody List<AnswerRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<List<AnswerResponse>>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo thành công")
                        .data(answerService.addBulkAnswers(questionId, requests))
                        .build());
    }
    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PutMapping("/{answerId}")
    public ResponseEntity<ApiResponse<AnswerResponse>> updateAnswer(
            @PathVariable Long questionId,
            @PathVariable Long answerId,
            @Valid @RequestBody AnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.<AnswerResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật thành công")
                .data(answerService.updateAnswer(questionId, answerId, request))
                .build());
    }
    @PreAuthorize("hasAuthority('QUESTION:DELETE')")
    @DeleteMapping("/{answerId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long questionId,
            @PathVariable Long answerId) {
        answerService.deleteAnswer(questionId, answerId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa thành công")
                .data(null)
                .build());
    }
}
