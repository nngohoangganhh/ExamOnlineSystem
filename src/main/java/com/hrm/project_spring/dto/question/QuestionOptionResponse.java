package com.hrm.project_spring.dto.question;

import com.hrm.project_spring.entity.QuestionOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QuestionOptionResponse {
    private Long id;

    private String content;

    private Boolean isCorrect;

    private BigDecimal score;

    public static QuestionOptionResponse from(QuestionOption questionOption) {
        if (questionOption == null) return null;
        return QuestionOptionResponse.builder()
                .id(questionOption.getId())
                .content(questionOption.getContent())
                .isCorrect(questionOption.getIsCorrect())
                .score(questionOption.getScoreWeight())
                .build();
    }
}
