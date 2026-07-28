package com.hrm.project_spring.dto.question;

import com.hrm.project_spring.enums.QuestionAction;
import com.hrm.project_spring.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "Loại câu hỏi không được để trống.")
    private QuestionType type;

    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    @Size(max = 5000, message = "Nội dung câu hỏi tối đa 5000 ký tự.")
    // Min 10 sẽ kiểm tra sau khi strip HTML trong Service
    private String stem;

    @NotNull(message = "Môn học không được để trống.")
    private Long subjectId;

    @NotNull(message = "Chương không được để trống.")
    private Long chapterId;

    @NotNull(message = "Mức độ Bloom không được để trống.")
    @Min(value = 1, message = "Mức độ Bloom phải từ 1 đến 6.")
    @Max(value = 6, message = "Mức độ Bloom phải từ 1 đến 6.")
    private Integer bloomLevel;

    @NotNull(message = "Điểm không được để trống.")
    @DecimalMin(value = "0.01", message = "Điểm phải từ 0.01 đến 100.")
    @DecimalMax(value = "100.00", message = "Điểm phải từ 0.01 đến 100.")
    private BigDecimal score;

    private List<QuestionOptionRequest> options;

    @Size(max = 10, message = "Tối đa 10 tag.")
    private List<@Size(min = 1, max = 30, message = "Mỗi tag từ 1 đến 30 ký tự.") String> tags;

    @Size(max = 3000, message = "Giải thích tối đa 3000 ký tự.")
    private String explanation;

    // Chỉ dùng cho Essay
    @Size(max = 5000, message = "Đáp án tham khảo tối đa 5000 ký tự.")
    private String referenceAnswer;

    // Chỉ dùng cho Essay
    @Size(max = 5000, message = "Rubric tối đa 5000 ký tự.")
    private String rubric;

    @NotNull(message = "Hành động không được để trống.")
    private QuestionAction action;

}