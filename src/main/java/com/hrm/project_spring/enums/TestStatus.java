package com.hrm.project_spring.enums;

/**
 * SRS v1.0 BR-044: Bài thi có 4 trạng thái.
 * Vòng đời: DRAFT → READY → OPEN → CLOSED
 */
public enum TestStatus {
    /** Bài thi đang được cấu hình, chưa đủ điều kiện mở. */
    DRAFT,
    /** Bài thi đã đủ điều kiện (có câu hỏi + thí sinh + lịch), sẵn sàng mở. */
    READY,
    /** Bài thi đang mở, thí sinh có thể vào thi (trong khoảng openTime–closeTime). */
    OPEN,
    /** Bài thi đã đóng, không nhận thêm lượt làm bài. */
    CLOSED,
    /** Bài thi đã lưu trữ. */
    ARCHIVED
}
