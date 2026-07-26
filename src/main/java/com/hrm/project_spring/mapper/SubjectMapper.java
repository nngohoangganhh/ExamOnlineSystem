package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.subject.SubjectResponse;
import com.hrm.project_spring.entity.Subject;

public class SubjectMapper {
    public static SubjectResponse toResponse(Subject subject) {
        if (subject == null) return null;
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
