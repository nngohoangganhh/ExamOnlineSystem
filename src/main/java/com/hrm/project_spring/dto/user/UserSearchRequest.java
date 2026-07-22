package com.hrm.project_spring.dto.user;

import com.hrm.project_spring.enums.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO tìm kiếm và lọc user — UC14.
 */
@Data
public class UserSearchRequest {

    /**
     * Từ khóa tìm kiếm theo tên, email, username, mã số.
     * 2-100 ký tự nếu có (E1 UC14).
     */
    @Size(min = 2, max = 100, message = "Từ khóa tìm kiếm phải từ 2 đến 100 ký tự")
    private String keyword;

    /** Lọc theo role id. */
    private Long roleId;

    /** Lọc theo classroom id. */
    private Long classId;

    /** Lọc theo trạng thái tài khoản. */
    private UserStatus status;

    /** Từ ngày tạo (inclusive). */
    private LocalDate createdFrom;

    /** Đến ngày tạo (inclusive). */
    private LocalDate createdTo;

    /** Có bao gồm user đã soft-delete không, mặc định false. */
    private boolean includeDeleted = false;
}
