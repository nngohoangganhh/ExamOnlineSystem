package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.ScoringPolicy;
import com.hrm.project_spring.enums.TestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SRS v1.0 §11.14: Đề thi (Test/Bài thi) — UC27–UC32.
 * Thay @ManyToMany questions bằng @OneToMany TestQuestion (junction entity có metadata).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tests")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false)
    private String title;

    /** UC28: Loại đề thi. VD: "MIDTERM", "FINAL", "PRACTICE". */
    @Column(length = 30)
    private String type;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** Tổng điểm tối đa của đề (dùng để hiển thị). */
    @Column(name = "total_score", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal totalScore = BigDecimal.ZERO;

    /** Điểm đạt (BR-049). */
    @Column(name = "passing_score", precision = 6, scale = 2)
    private BigDecimal passingScore;

    /** Số lượt làm bài tối đa. null = không giới hạn (UC28). */
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    /** Thời gian chờ giữa các lượt (phút). 0 = không chờ (UC28). */
    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 0;

    /** BR-050: Chính sách tính điểm khi có nhiều lượt. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_policy", length = 20)
    @Builder.Default
    private ScoringPolicy scoringPolicy = ScoringPolicy.HIGHEST;

    /** UC28: Xáo trộn thứ tự câu hỏi. */
    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    private Boolean shuffleQuestions = false;

    /** UC28: Xáo trộn thứ tự đáp án. */
    @Column(name = "shuffle_options", nullable = false)
    @Builder.Default
    private Boolean shuffleOptions = false;

    /** UC39: Hiển thị kết quả ngay sau khi nộp. */
    @Column(name = "show_result_immediately", nullable = false)
    @Builder.Default
    private Boolean showResultImmediately = false;

    /** UC41: Cho phép xem lại bài sau khi nộp. */
    @Column(name = "allow_review_after_submit", nullable = false)
    @Builder.Default
    private Boolean allowReviewAfterSubmit = false;

    /** UC41: Hiển thị đáp án đúng khi xem lại. */
    @Column(name = "show_correct_answers", nullable = false)
    @Builder.Default
    private Boolean showCorrectAnswers = false;

    /**
     * UC28: Cấu hình chống gian lận (JSON string).
     * VD: {"tabSwitchLimit": 3, "fullScreenRequired": true}
     */
    @Column(name = "anti_cheat_config", columnDefinition = "TEXT")
    private String antiCheatConfig;

    /** UC31: Thời điểm mở bài thi. null = mở ngay khi OPEN. */
    @Column(name = "open_time")
    private LocalDateTime openTime;

    /** UC31: Thời điểm đóng bài thi. null = không tự đóng. */
    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TestStatus status = TestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Người phụ trách bài thi (có thể khác người tạo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * UC29: Danh sách câu hỏi của đề thi kèm metadata (thứ tự, điểm từng câu).
     * Thay thế @ManyToMany để lưu được order_num và score per question.
     */
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestQuestion> testQuestions = new ArrayList<>();

    /**
     * UC30: Danh sách enrollment (thí sinh được gán vào bài thi).
     */
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();
}