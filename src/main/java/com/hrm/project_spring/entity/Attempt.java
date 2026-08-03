package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SRS v1.0 §11.16: Lượt làm bài (Attempt) — UC34–UC41.
 * Mỗi lần thí sinh vào thi tạo 1 Attempt mới.
 */
@Entity
@Table(name = "attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Lượt thứ mấy của thí sinh này trong bài thi này. */
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /** UC34: Thời điểm bắt đầu làm bài. */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** UC34: Thời điểm dự kiến kết thúc = startedAt + durationMinutes. */
    @Column(name = "scheduled_end_at", nullable = false)
    private LocalDateTime scheduledEndAt;

    /** UC39/UC40: Thời điểm nộp bài. */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * UC40: Lý do nộp.
     * "MANUAL" = thí sinh tự nộp, "AUTO_SUBMIT" = hết giờ server nộp.
     */
    @Column(name = "submit_reason", length = 20)
    private String submitReason;

    /** Điểm thô (trước khi scale nếu có). */
    @Column(name = "raw_score", precision = 6, scale = 2)
    private BigDecimal rawScore;

    /** Điểm cuối cùng (sau scale hoặc chấm tay). */
    @Column(name = "final_score", precision = 6, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    // ─── Anti-cheat tracking ──────────────────────────────────────────────────

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Số lần chuyển tab / mất focus. */
    @Builder.Default
    @Column(name = "tab_switch_count", nullable = false)
    private Integer tabSwitchCount = 0;

    /** JSON log chi tiết các sự kiện gian lận (text để tránh phụ thuộc JSONB). */
    @Column(name = "anti_cheat_log", columnDefinition = "TEXT")
    private String antiCheatLog;

    /** Các câu trả lời trong lượt làm bài này. */
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AttemptAnswer> answers = new ArrayList<>();
}
