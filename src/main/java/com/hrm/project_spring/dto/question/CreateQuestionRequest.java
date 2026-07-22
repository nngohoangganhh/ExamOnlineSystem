package com.hrm.project_spring.dto.question;

import com.hrm.project_spring.enums.QuestionAction;
import com.hrm.project_spring.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {


    private QuestionType type;

    private String stem;

    private Long subjectId;
    // Chương
    private Long chapterId;

    private Integer bloomLevel;

    private BigDecimal score;

    private List<QuestionOptionRequest> options;

    private List<String> tags;

    private String explanation;

    // Chỉ dùng cho Essay
    private String referenceAnswer;
    // Chỉ dùng cho Essay
    private String rubric;


    private QuestionAction action;
}