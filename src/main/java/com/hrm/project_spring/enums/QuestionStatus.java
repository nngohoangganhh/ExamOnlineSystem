package com.hrm.project_spring.enums;

/**
 * Trạng thái câu hỏi theo SRS v1.0 UC16–UC23.
 * PENDING bị xóa (trùng với PENDING_REVIEW).
 */
public enum QuestionStatus {
    /** Câu hỏi mới tạo, chưa nộp duyệt. */
    DRAFT,
    /** Đã nộp duyệt, chờ giảng viên review. */
    PENDING_REVIEW,
    /** Đã duyệt, sẵn sàng dùng trong bài thi. */
    APPROVED,
    /** Bị từ chối. Cần chỉnh sửa lại. */
    REJECTED,
    /** Đã lưu trữ (không còn dùng mới, nhưng vẫn hiện trong kết quả cũ). */
    ARCHIVED
}
