package com.hrm.project_spring.dto.question;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionOptionResponse {
    private Long id;

    private String content;

    private Boolean isCorrect;

    private BigDecimal score;
}
