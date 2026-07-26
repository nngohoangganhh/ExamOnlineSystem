package com.hrm.project_spring.dto.question;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionClassificationRequest {

    @NotEmpty(message = "Danh sách ID câu hỏi không được để trống.")
    private List<Long> questionIds;

    @NotNull(message = "Môn học ID không được để trống.")
    private Long subjectId;

    @NotNull(message = "Chương ID không được để trống.")
    private Long chapterId;
}
