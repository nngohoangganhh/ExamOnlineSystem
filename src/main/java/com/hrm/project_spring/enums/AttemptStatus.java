package com.hrm.project_spring.enums;

/** SRS v1.0 §11.16: Trạng thái lượt làm bài (Attempt). */
public enum AttemptStatus {
    /** Thí sinh đang làm bài. */
    IN_PROGRESS,
    /** Thí sinh tự nộp bài. */
    SUBMITTED,
    /** Server tự động nộp khi hết giờ (UC40). */
    AUTO_SUBMITTED,
    /** Bài đã được chấm điểm (bao gồm tự luận). */
    GRADED
}
