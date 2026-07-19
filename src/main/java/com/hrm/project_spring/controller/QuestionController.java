package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.QuestionRequest;
import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getAllQuestion(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<QuestionResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách thành công")
                .data(questionService.getAllQuestion(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiết câu hỏi")
                .data(questionService.getQuestionById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('QUESTION:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo câu hỏi thành công")
                .data(questionService.create(request))
                .build());
    }

    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @PathVariable Long id,
            @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật thành công")
                .data(questionService.update(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('QUESTION:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa câu hỏi thành công")
                .data(null)
                .build());
    }
}
