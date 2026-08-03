package com.hrm.project_spring.enums;

/** SRS v1.0: Trạng thái bài thi (Test/Đề thi). */
public enum TestStatus {
    /** Bài thi đang được cấu hình, chưa mở. */
    DRAFT,
    /** Bài thi đang mở, thí sinh có thể vào thi (trong khoảng openTime–closeTime). */
    OPEN,
    /** Bài thi đã đóng, không nhận thêm lượt làm bài. */
    CLOSED,
    /** Bài thi đã lưu trữ. */
    ARCHIVED
}
