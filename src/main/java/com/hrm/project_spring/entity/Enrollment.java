package com.hrm.project_spring.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SRS v1.0 §11.15: Enrollment — gán thí sinh vào bài thi (UC30).
 * Mỗi enrollment = 1 thí sinh có quyền làm bài thi đó, với số lượt được phép.
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * BR-051: Số lượt đã sử dụng.
     * Tăng 1 mỗi khi tạo Attempt mới.
     */
    @Builder.Default
    @Column(name = "attempts_used", nullable = false)
    private Integer attemptsUsed = 0;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    /** Người thực hiện gán (Teacher/Admin). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
