package com.hrm.project_spring.dto.attempt;

import com.hrm.project_spring.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswerResponse {
    private Long questionId;
    private String stem;
    private QuestionType type;
    private List<OptionView> options;
    private String answerData;
    private Boolean isCorrect;
    private BigDecimal score;
    private Boolean markedForReview;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionView {
        private Long id;
        private String content;
        private Boolean isCorrect;
    }
}
