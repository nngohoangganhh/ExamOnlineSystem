package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.*;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.service.question.QuestionExportService;
import com.hrm.project_spring.service.question.QuestionImportService;
import com.hrm.project_spring.service.question.QuestionReviewService;
import com.hrm.project_spring.service.question.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionReviewService questionReviewService;
    private final QuestionImportService questionImportService;
    private final QuestionExportService questionExportService;

    // ─── UC16, UC24: Lấy danh sách / tìm kiếm câu hỏi ───────────────────────

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> search(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Integer bloomLevel,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<QuestionResponse>>builder()
                .success(true).code(200)
                .message("Lấy danh sách thành công")
                .data(questionService.search(subjectId, chapterId, bloomLevel,
                        status, keyword, tag, pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<QuestionDetailResponse>builder()
                .success(true).code(200)
                .message("Chi tiết câu hỏi")
                .data(questionService.getQuestionById(id))
                .build());
    }

    // ─── UC16: Tạo câu hỏi ───────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true).code(201)
                .message("Tạo câu hỏi thành công")
                .data(questionService.create(request))
                .build());
    }

    // ─── UC17: Cập nhật câu hỏi ──────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateQuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.<QuestionResponse>builder()
                .success(true).code(200)
                .message("Cập nhật thành công")
                .data(questionService.update(id, request))
                .build());
    }

    // ─── UC18: Xóa câu hỏi (soft delete) ────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).code(200)
                .message("Xóa câu hỏi thành công")
                .data(null)
                .build());
    }

    // ─── UC18: Archive câu hỏi ───────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:DELETE')")
    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long id) {
        questionService.archive(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).code(200)
                .message("Lưu trữ câu hỏi thành công")
                .data(null)
                .build());
    }

    // ─── UC23: Duyệt câu hỏi ─────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:APPROVE')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id,
                                                      HttpServletRequest request) {
        questionReviewService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).code(200)
                .message("Duyệt câu hỏi thành công")
                .data(null)
                .build());
    }

    // ─── UC23: Từ chối câu hỏi ───────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:APPROVE')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id,
                                                     @RequestParam String reason,
                                                     HttpServletRequest request) {
        questionReviewService.reject(id, reason, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).code(200)
                .message("Từ chối câu hỏi thành công")
                .data(null)
                .build());
    }

    // ─── UC23: Duyệt hàng loạt ───────────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:APPROVE')")
    @PostMapping("/bulk-approve")
    public ResponseEntity<ApiResponse<Integer>> bulkApprove(@RequestBody List<Long> questionIds,
                                                              HttpServletRequest request) {
        int approved = questionReviewService.bulkApprove(questionIds, request);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .success(true).code(200)
                .message("Duyệt " + approved + " câu hỏi thành công")
                .data(approved)
                .build());
    }

    // ─── UC19: Import câu hỏi từ Excel ───────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:CREATE')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionImportResultResponse>> importQuestions(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        var result = questionImportService.importFromExcel(file, request);
        return ResponseEntity.ok(ApiResponse.<QuestionImportResultResponse>builder()
                .success(true).code(200)
                .message("Import hoàn tất: " + result.imported() + " câu hỏi, " +
                         result.errors().size() + " lỗi")
                .data(new QuestionImportResultResponse(result.imported(), result.errors()))
                .build());
    }

    // ─── UC20: Export câu hỏi ra Excel ───────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Integer bloomLevel,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            HttpServletRequest request) {
        byte[] bytes = questionExportService.exportToExcel(
                subjectId, chapterId, bloomLevel, status, keyword, tag, request);

        String filename = "questions_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ─── Bulk update classification ───────────────────────────────────────────

    @PreAuthorize("hasAuthority('QUESTION:UPDATE')")
    @PatchMapping("/classification")
    public ResponseEntity<ApiResponse<Integer>> updateQuestionClassification(
            @Valid @RequestBody QuestionClassificationRequest request) {
        int updatedCount = questionService.updateQuestionClassification(request);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .success(true).code(200)
                .message("Cập nhật môn học và chương cho các câu hỏi thành công")
                .data(updatedCount)
                .build());
    }
}
