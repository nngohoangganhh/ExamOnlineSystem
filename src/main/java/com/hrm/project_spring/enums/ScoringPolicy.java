package com.hrm.project_spring.enums;

/**
 * SRS v1.0 §5.28 BR-050: Chính sách tính điểm khi có nhiều lượt làm bài.
 */
public enum ScoringPolicy {
    /** Tính điểm lượt đầu tiên. */
    FIRST,
    /** Tính điểm lượt cuối cùng. */
    LAST,
    /** Tính điểm cao nhất trong các lượt. */
    HIGHEST,
    /** Tính điểm trung bình tất cả các lượt. */
    AVERAGE
}
