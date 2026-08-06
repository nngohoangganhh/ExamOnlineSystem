package com.hrm.project_spring.dto.test;

import com.hrm.project_spring.enums.TestStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response trả về chi tiết bài thi (Test).
 * UC27, UC28 — không expose isCorrect để bảo vệ đáp án đúng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {

    private Long id;
    private Long examId;
    private String examName;
    private String title;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private BigDecimal passingScore;
    private TestStatus status;
    private Integer maxAttempts;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean showResultImmediately;
    private Boolean allowReviewAfterSubmit;

    /** Thời điểm tạo bài thi. */
    private LocalDateTime createdAt;

    /** Danh sách câu hỏi trong đề thi (chỉ trả nội dung, không trả isCorrect). */
    private List<QuestionDto> questions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private String stem;
        private String type;
        private Integer bloomLevel;
        private List<AnswerDto> answers;
        private Integer orderNum;
        private BigDecimal score;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDto {
        private Long id;
        private String content;
        // isCorrect KHÔNG expose — bảo vệ đáp án đúng với student
    }
}
