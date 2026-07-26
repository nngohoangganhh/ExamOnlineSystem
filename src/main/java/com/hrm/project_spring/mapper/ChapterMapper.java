package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.chapter.ChapterResponse;
import com.hrm.project_spring.entity.Chapter;

public class ChapterMapper {
    public static ChapterResponse toResponse(Chapter chapter) {
        if (chapter == null) return null;
        return ChapterResponse.builder()
                .id(chapter.getId())
                .subjectId(chapter.getSubject() != null ? chapter.getSubject().getId() : null)
                .subjectName(chapter.getSubject() != null ? chapter.getSubject().getName() : null)
                .name(chapter.getName())
                .code(chapter.getCode())
                .order(chapter.getOrderNum())
                .description(chapter.getDescription())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
