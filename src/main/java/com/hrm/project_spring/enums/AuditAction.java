package com.hrm.project_spring.enums;

/**
 * UC07: Các loại sự kiện được ghi vào Audit Log.
 */
public enum AuditAction {
    // Auth
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGE,
    PASSWORD_RESET,
    TOKEN_REFRESH,

    // User management
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    USER_RESTORE,
    USER_LOCK,
    USER_UNLOCK,
    USER_ACTIVATE,
    USER_EXPORT,
    USER_IMPORT,

    // Role & Permission
    ROLE_CREATE,
    ROLE_UPDATE,
    ROLE_DELETE,
    ROLE_ASSIGN,
    ROLE_REVOKE,

    // Class
    CLASS_CREATE,
    CLASS_UPDATE,
    CLASS_DELETE,
    CLASS_ASSIGN_STUDENT,
    CLASS_REMOVE_STUDENT
}
