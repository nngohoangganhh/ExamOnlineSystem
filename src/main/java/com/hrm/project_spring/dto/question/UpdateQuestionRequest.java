package com.hrm.project_spring.dto.question;

import com.hrm.project_spring.enums.QuestionAction;
import com.hrm.project_spring.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateQuestionRequest {

    @NotBlank
    private String stem;

    @NotNull
    private QuestionType type;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long chapterId;

    @NotNull
    @Min(1)
    @Max(6)
    private Integer bloomLevel;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("100")
    private BigDecimal score;

    @Size(max = 3000)
    private String explanation;

    private String referenceAnswer;

    private String rubric;

    @Valid
    private List<QuestionOptionRequest> options;

    @NotNull
    private QuestionAction action;
}