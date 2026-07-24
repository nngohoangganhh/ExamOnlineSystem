package com.hrm.project_spring.dto.answer;

import  jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerRequest {
    @NotBlank(message = "Nội dung đáp án không được để trống")
    private String content;

    @NotNull(message = "Trạng thái đáp án đúng/sai không được để trống")
    private Boolean isCorrect;
}
