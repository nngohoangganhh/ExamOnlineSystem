package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.test.AssignQuestionsRequest;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.service.TestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TestResponse>>> getAllTests(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<TestResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách thành công")
                .data(testService.getAllTest(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiết test")
                .data(testService.getTestById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponse>> createTest(@RequestBody TestRequest request) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo test thành công")
                .data(testService.createTest(request))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> updateTest(@PathVariable Long id, @RequestBody TestRequest request) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật test thành công")
                .data(testService.updateTest(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(@PathVariable Long id) {
        testService.deleteTest(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa test thành công")
                .data(null)
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/questions")
    public ResponseEntity<ApiResponse<TestResponse>> assignQuestions(
            @PathVariable Long testId,
            @Valid @RequestBody AssignQuestionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Gán câu hỏi vào test thành công")
                .data(testService.assignQuestions(testId, request))
                .build());
    }
}
