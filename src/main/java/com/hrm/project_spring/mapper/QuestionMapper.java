package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.entity.Question;

public class QuestionMapper {
    public static QuestionResponse toResponse(Question question) {
        if (question == null) return null;
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .createdBy(question.getCreatedBy() != null ? question.getCreatedBy().getFullName() : null)
                .createdAt(question.getCreatedAt())
                .build();
    }
}
