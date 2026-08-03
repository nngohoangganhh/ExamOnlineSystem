package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SRS v1.0 §11.11: Câu hỏi trong Ngân hàng câu hỏi (UC16–UC24).
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String stem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private Integer bloomLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    // Essay
    @Column(columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(columnDefinition = "TEXT")
    private String rubric;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuestionOption> questionOptions = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "question_tags",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ─── SRS v1.0: thêm mới ───────────────────────────────────────────────────

    /** UC18: Soft delete — không xóa vật lý. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** UC23: Người đã duyệt câu hỏi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    /** UC23: Thời điểm duyệt. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** UC23: Lý do từ chối (nếu REJECTED). */
    @Column(name = "rejection_comment", columnDefinition = "TEXT")
    private String rejectionComment;

    /**
     * BR-033: Đếm số lần câu hỏi được dùng trong bài thi.
     * Nếu > 0 thì không cho xóa vật lý, chỉ được archive.
     */
    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}