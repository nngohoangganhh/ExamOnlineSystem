package com.hrm.project_spring.enums;

/**
 * UC07 + SRS v1.0: Toàn bộ các loại sự kiện được ghi vào Audit Log.
 */
public enum AuditAction {
    // ─── Auth ────────────────────────────────────────────────────────────────
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGE,
    PASSWORD_RESET,
    TOKEN_REFRESH,

    // ─── User management ─────────────────────────────────────────────────────
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    USER_RESTORE,
    USER_LOCK,
    USER_UNLOCK,
    USER_ACTIVATE,
    USER_EXPORT,
    USER_IMPORT,

    // ─── Role & Permission ────────────────────────────────────────────────────
    ROLE_CREATE,
    ROLE_UPDATE,
    ROLE_DELETE,
    ROLE_ASSIGN,
    ROLE_REVOKE,

    // ─── Class ───────────────────────────────────────────────────────────────
    CLASS_CREATE,
    CLASS_UPDATE,
    CLASS_DELETE,
    CLASS_ASSIGN_STUDENT,
    CLASS_REMOVE_STUDENT,

    // ─── Question Bank (UC16–UC24) ────────────────────────────────────────────
    QUESTION_CREATE,
    QUESTION_UPDATE,
    QUESTION_DELETE,
    QUESTION_APPROVE,
    QUESTION_REJECT,
    QUESTION_ARCHIVE,
    QUESTION_IMPORT,
    QUESTION_EXPORT,

    // ─── Exam Management (UC25–UC32) ─────────────────────────────────────────
    EXAM_CREATE,
    EXAM_UPDATE,
    EXAM_DELETE,
    TEST_CREATE,
    TEST_UPDATE,
    TEST_DELETE,
    TEST_PUBLISH,
    TEST_CLOSE,
    TEST_EXPORT_PDF,

    // ─── Participation (UC33–UC41) ────────────────────────────────────────────
    ATTEMPT_START,
    ATTEMPT_SUBMIT,
    ATTEMPT_AUTO_SUBMIT,

    // ─── Reports (UC42–UC48) ──────────────────────────────────────────────────
    REPORT_EXPORT,
    CERTIFICATE_GENERATE
}
