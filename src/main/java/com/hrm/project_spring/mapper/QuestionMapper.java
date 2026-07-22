package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.entity.Question;

public class QuestionMapper {
    public static QuestionResponse toResponse(Question question) {
        if (question == null) return null;
        return QuestionResponse.builder()
                .id(question.getId())
                // stem → content (DTO field), type.name() → questionType (String DTO)
                .content(question.getStem())
                .questionType(question.getType() != null ? question.getType().name() : null)
                // bloomLevel → difficulty (dạng số → String)
                .difficulty(question.getBloomLevel() != null ? String.valueOf(question.getBloomLevel()) : null)
                // createdBy là Long (user id), không phải User entity
                .createdBy(question.getCreatedBy() != null ? String.valueOf(question.getCreatedBy()) : null)
                .createdAt(question.getCreatedAt())
                .build();
    }
}
