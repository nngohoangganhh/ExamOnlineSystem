package com.hrm.project_spring.dto.test;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {
    private Long id;
    private Long examId;
    private String title;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalTime createAt;
    private List<QuestionDto> questions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private String content;
        private String difficulty;
        private List<AnswerDto> answers;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDto {
        private Long id;
        private String content;
        // KHÔNG expose isCorrect cho student (chỉ cần khi admin)
    }
}
