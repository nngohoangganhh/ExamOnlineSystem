package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.test.TestSummaryResponse;
import com.hrm.project_spring.dto.test.AssignQuestionsRequest;
import com.hrm.project_spring.dto.test.TestQuestionResponse;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.service.TestQuestionService;
import com.hrm.project_spring.service.TestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho Bài thi (Test) — UC27–UC31.
 *
 * <p>Thiết kế theo chuẩn RESTful:</p>
 * <ul>
 *   <li>URL chỉ chứa danh từ, không chứa động từ (add, remove, detail).</li>
 *   <li>HTTP method thể hiện hành động: GET=đọc, POST=tạo mới, PUT=thay thế, DELETE=xóa.</li>
 *   <li>Tạo mới trả về 201 Created; các action thành công trả về 200 OK.</li>
 *   <li>@Valid kích hoạt Bean Validation trên toàn bộ @RequestBody.</li>
 * </ul>
 *
 * <pre>
 * GET    /api/tests                           → Danh sách bài thi (phân trang)
 * GET    /api/tests/{id}                      → Chi tiết bài thi
 * POST   /api/tests                           → Tạo bài thi mới          (201 Created)
 * PUT    /api/tests/{id}                      → Cập nhật bài thi
 * DELETE /api/tests/{id}                      → Xóa bài thi (soft delete)
 *
 * GET    /api/tests/{testId}/questions        → Danh sách câu hỏi trong bài thi
 * PUT    /api/tests/{testId}/questions        → Thay thế toàn bộ danh sách câu hỏi
 * POST   /api/tests/{testId}/questions        → Thêm câu hỏi vào bài thi
 * DELETE /api/tests/{testId}/questions/{qId} → Xóa một câu hỏi khỏi bài thi
 * </pre>
 */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final TestQuestionService testQuestionService;

    // ======================== TEST CRUD ========================

    /**
     * GET /api/tests — Lấy danh sách bài thi có phân trang.
     */
    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TestSummaryResponse>>> getAllTests(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<TestSummaryResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách bài thi thành công")
                .data(testService.getAllTest(pageNo, pageSize))
                .build());
    }

    /**
     * GET /api/tests/{id} — Lấy chi tiết một bài thi.
     */
    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết bài thi thành công")
                .data(testService.getTestById(id))
                .build());
    }

    /**
     * POST /api/tests — Tạo bài thi mới.
     * Trả về 201 Created theo chuẩn RESTful (tạo resource mới).
     */
    @PreAuthorize("hasAuthority('TEST:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponse>> createTest(
            @Valid @RequestBody TestRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<TestResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo bài thi thành công")
                        .data(testService.createTest(request))
                        .build());
    }

    /**
     * PUT /api/tests/{id} — Cập nhật toàn bộ thông tin bài thi.
     */
    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResponse>> updateTest(
            @PathVariable Long id,
            @Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật bài thi thành công")
                .data(testService.updateTest(id, request))
                .build());
    }

    /**
     * DELETE /api/tests/{id} — Xóa bài thi (soft delete).
     */
    @PreAuthorize("hasAuthority('TEST:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(@PathVariable Long id) {
        testService.deleteTest(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa bài thi thành công")
                .data(null)
                .build());
    }

    // ======================== QUESTIONS SUB-RESOURCE ========================

    /**
     * GET /api/tests/{testId}/questions — Lấy danh sách câu hỏi trong bài thi.
     * Trả DTO {@link TestQuestionResponse} thay vì entity thô để bảo vệ đáp án đúng.
     */
    @PreAuthorize("hasAuthority('TEST:READ')")
    @GetMapping("/{testId}/questions")
    public ResponseEntity<ApiResponse<List<TestQuestionResponse>>> getTestQuestions(
            @PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.<List<TestQuestionResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách câu hỏi của bài thi thành công")
                .data(testQuestionService.getTestQuestions(testId))
                .build());
    }

    /**
     * PUT /api/tests/{testId}/questions — Thay thế toàn bộ danh sách câu hỏi.
     * Xóa tất cả câu hỏi cũ rồi gán câu hỏi mới (semantics: replace collection).
     */
    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PutMapping("/{testId}/questions")
    public ResponseEntity<ApiResponse<TestResponse>> replaceQuestions(
            @PathVariable Long testId,
            @Valid @RequestBody AssignQuestionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<TestResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật danh sách câu hỏi thành công")
                .data(testService.assignQuestions(testId, request))
                .build());
    }

    /**
     * POST /api/tests/{testId}/questions — Thêm câu hỏi vào bài thi.
     * Không xóa câu hỏi cũ (semantics: append to collection).
     * Chỉ gán câu hỏi có status=APPROVED (BR-030).
     */
    @PreAuthorize("hasAuthority('TEST:UPDATE')")
    @PostMapping("/{testId}/questions")
    public ResponseEntity<ApiResponse<Void>> addQuestions(
            @PathVariable Long testId,
            @RequestBody List<Long> questionIds,
            HttpServletRequest request) {
        testQuestionService.addQuestions(testId, questionIds, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Thêm câu hỏi vào bài thi thành công")
                .data(null)
                .build());
    }

    /**
     * DELETE /api/tests/{testId}/questions/{questionId} — Xóa một câu hỏi khỏi bài thi.
     */
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
                .message("Xóa câu hỏi khỏi bài thi thành công")
                .data(null)
                .build());
    }
}
