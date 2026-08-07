package com.hrm.project_spring.dto.test;

import com.hrm.project_spring.enums.ScoringPolicy;
import com.hrm.project_spring.enums.TestStatus;
import com.hrm.project_spring.enums.TestType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SRS v1.0 UC27/UC28/UC29: Response chi tiết bài thi kèm danh sách câu hỏi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {

    private Long id;
    private Long examId;
    private String title;

    /** UC27: Loại bài thi. */
    private TestType type;

    /** UC28: Trạng thái bài thi. */
    private TestStatus status;

    private Integer durationMinutes;

    /** Tổng điểm tối đa của đề (BigDecimal để khớp với entity). */
    private BigDecimal totalScore;

    /** UC28: Điểm đạt. */
    private BigDecimal passingScore;

    /** UC28: Số lần thi tối đa. */
    private Integer maxAttempts;

    /** UC28: Chính sách tính điểm. */
    private ScoringPolicy scoringPolicy;

    /** UC28: Xáo trộn câu hỏi. */
    private Boolean shuffleQuestions;

    /** UC28: Xáo trộn đáp án. */
    private Boolean shuffleOptions;

    /** UC31: Thời điểm mở bài thi. */
    private LocalDateTime openTime;

    /** UC31: Thời điểm đóng bài thi. */
    private LocalDateTime closeTime;

    /** Thời điểm tạo bài thi. */
    private LocalDateTime createdAt;

    /** UC29: Danh sách câu hỏi của bài thi. */
    private List<QuestionDto> questions;

    // ─── Inner DTOs ────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;

        /** Nội dung câu hỏi (stem). */
        private String content;

        /** Loại câu hỏi (MCQ_SINGLE, MCQ_MULTIPLE, ESSAY, TRUE_FALSE). */
        private String type;

        /** Mức độ Bloom (1-6). */
        private Integer bloomLevel;

        /** Điểm của câu hỏi này trong đề thi. */
        private BigDecimal score;

        /** Thứ tự trong đề thi (1-based). */
        private Integer orderNum;

        /** Danh sách đáp án (KHÔNG expose isCorrect cho student). */
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
        // KHÔNG expose isCorrect cho student — chỉ trả khi admin/teacher xem lại
    }
}

