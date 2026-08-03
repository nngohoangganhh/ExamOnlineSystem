package com.hrm.project_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * SRS v1.0 §11.17: Junction table giữa Test và Question.
 * Lưu thêm thứ tự (order_num) và điểm từng câu (score) trong một đề cụ thể.
 * Thay thế @ManyToMany test_questions cũ.
 */
@Entity
@Table(name = "test_questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "question_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Đề thi chứa câu hỏi này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    /** Câu hỏi được gán. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Thứ tự hiển thị trong đề (1-based). */
    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    /**
     * Điểm của câu hỏi này trong đề thi.
     * BR-049: Có thể khác score mặc định của câu hỏi (để scale theo tổng điểm đề).
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;
}
