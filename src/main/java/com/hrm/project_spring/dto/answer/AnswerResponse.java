package com.hrm.project_spring.dto.answer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerResponse {
    private Long id;
    private Long questionId;
    private String content;
    private Boolean isCorrect;
}
