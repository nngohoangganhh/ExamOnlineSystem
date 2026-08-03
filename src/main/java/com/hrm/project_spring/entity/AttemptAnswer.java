package com.hrm.project_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SRS v1.0 §11.17: Câu trả lời của thí sinh trong một lượt làm bài (AttemptAnswer).
 * UC35: Trả lời câu hỏi; UC36: Lưu tạm; UC38: Đánh dấu xem lại.
 */
@Entity
@Table(name = "attempt_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * UC35: Dữ liệu trả lời dạng JSON string.
     * MCQ_SINGLE: {"selectedOptionId": 12}
     * MCQ_MULTIPLE: {"selectedOptionIds": [12, 13]}
     * TRUE_FALSE: {"selectedOptionId": 15}
     * ESSAY: {"text": "Nội dung tự luận..."}
     */
    @Column(name = "answer_data", columnDefinition = "TEXT")
    private String answerData;

    /** UC39: Câu trả lời có đúng không (null nếu chưa chấm / tự luận chờ chấm). */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    /** Điểm của câu này (null nếu chưa chấm). */
    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    /** UC38: Đánh dấu câu cần xem lại. */
    @Builder.Default
    @Column(name = "marked_for_review", nullable = false)
    private Boolean markedForReview = false;

    /** Thời điểm lưu lần đầu. */
    @Column(name = "first_saved_at")
    private LocalDateTime firstSavedAt;

    /** UC36: Thời điểm lưu lần cuối (auto-save cập nhật). */
    @Column(name = "last_saved_at")
    private LocalDateTime lastSavedAt;

    /** Người chấm tay (nếu là tự luận). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    /** Nhận xét của người chấm. */
    @Column(name = "grading_comment", columnDefinition = "TEXT")
    private String gradingComment;
}
