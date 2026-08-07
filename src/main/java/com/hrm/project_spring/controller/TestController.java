package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.TestSummaryResponse;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.dto.test.TestScheduleRequest;
import com.hrm.project_spring.entity.TestQuestion;
import com.hrm.project_spring.service.TestQuestionService;
import com.hrm.project_spring.service.TestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;
    private final TestQuestionService testQuestionService;

    public TestController(TestService testService, TestQuestionService testQuestionService) {
        this.testService = testService;
        this.testQuestionService = testQuestionService;
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TestSummaryResponse>>> getAllTests(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<TestSummaryResponse>>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testService.getAllTest(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Chi tiáº¿t bÃ i thi")
                .data(testService.getTestById(id))
                .build());
    }


    @PreAuthorize("hasAuthority('TEST:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponse>> createTest(
            @Valid @RequestBody TestRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<TestResponse>builder()
                        .success(true)
                        .code(201)
                        .message("")
                        .data(testService.createTest(request, httpRequest))
                        .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> updateTest(
            @PathVariable Long id,
            @Valid @RequestBody TestRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testService.updateTest(id, request, httpRequest))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        testService.deleteTest(id, httpRequest);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("")
                .data(null)
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/schedule")
    public ResponseEntity<ApiResponse<TestResponse>> schedule(
            @PathVariable Long testId,
            @Valid @RequestBody TestScheduleRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testService.schedule(testId, request, httpRequest))
                .build());
    }


    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/close")
    public ResponseEntity<ApiResponse<TestResponse>> closeNow(
            @PathVariable Long testId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testService.closeNow(testId, httpRequest))
                .build());
    }


    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/archive")
    public ResponseEntity<ApiResponse<TestResponse>> archive(
            @PathVariable Long testId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testService.archive(testId, httpRequest))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/questions/add")
    public ResponseEntity<ApiResponse<Void>> addQuestions(
            @PathVariable Long testId,
            @RequestBody List<Long> questionIds,
            HttpServletRequest request) {
        testQuestionService.addQuestions(testId, questionIds, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("")
                .build());
    }


    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @DeleteMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> removeQuestion(
            @PathVariable Long testId,
            @PathVariable Long questionId,
            HttpServletRequest request) {
        testQuestionService.removeQuestion(testId, questionId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("")
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{testId}/questions/detail")
    public ResponseEntity<ApiResponse<List<TestQuestion>>> getTestQuestions(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.<List<TestQuestion>>builder()
                .success(true)
                .code(200)
                .message("")
                .data(testQuestionService.getTestQuestions(testId))
                .build());
    }

    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{testId}/export-pdf")
    public ResponseEntity<ApiResponse<String>> exportPdf(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "student") String version,
            @RequestParam(defaultValue = "1") int codeCount,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .code(200)
                .message("")
                .data(null)
                .build());
    }
}
