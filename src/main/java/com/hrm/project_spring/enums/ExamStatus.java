package com.hrm.project_spring.enums;

/** SRS v1.0: Trạng thái kỳ thi (Exam). */
public enum ExamStatus {
    /** Kỳ thi đang được soạn thảo. */
    DRAFT,
    /** Kỳ thi đã được công bố (các bài thi có thể được mở). */
    PUBLISHED,
    /** Kỳ thi đã kết thúc/lưu trữ. */
    ARCHIVED
}
