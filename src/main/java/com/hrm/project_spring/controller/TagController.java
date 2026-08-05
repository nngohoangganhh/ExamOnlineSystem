package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.entity.Tag;
import com.hrm.project_spring.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;

    @PreAuthorize("hasAuthority('QUESTION:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return ResponseEntity.ok(ApiResponse.<List<Tag>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách tag thành công")
                .data(tags)
                .build());
    }
}
