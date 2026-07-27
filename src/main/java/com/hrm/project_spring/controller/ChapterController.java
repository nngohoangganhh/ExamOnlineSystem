package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.chapter.ChapterRequest;
import com.hrm.project_spring.dto.chapter.ChapterResponse;
import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChapterController {

    private final ChapterService chapterService;

    @PreAuthorize("hasAuthority('CHAPTER:CREATE')")
    @PostMapping("/chapters")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(@Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(ApiResponse.<ChapterResponse>builder()
                .success(true)
                .code(201)
                .message("Tạo chương thành công")
                .data(chapterService.createChapter(request))
                .build());
    }

    @PreAuthorize("hasAuthority('CHAPTER:READ')")
    @GetMapping("/chapters")
    public ResponseEntity<ApiResponse<PageResponse<ChapterResponse>>> getAllChapters(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ChapterResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách chương thành công")
                .data(chapterService.getAllChapters(pageNo, pageSize))
                .build());
    }

    @PreAuthorize("hasAuthority('CHAPTER:READ')")
    @GetMapping("chapters/{id}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ChapterResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết chương thành công")
                .data(chapterService.getChapterById(id))
                .build());
    }

    @PreAuthorize("hasAuthority('CHAPTER:READ')")
    @GetMapping("/subjects/{subjectId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersBySubjectId(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.<List<ChapterResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách chương theo môn học thành công")
                .data(chapterService.getChaptersBySubjectId(subjectId))
                .build());
    }

    @PreAuthorize("hasAuthority('CHAPTER:UPDATE')")
    @PutMapping("chapters/{id}")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(ApiResponse.<ChapterResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật chương thành công")
                .data(chapterService.updateChapter(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('CHAPTER:DELETE')")
    @DeleteMapping("chapters/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa chương thành công")
                .build());
    }
}
