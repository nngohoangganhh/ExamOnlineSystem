package com.hrm.project_spring.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LockedRequest {
    @NotNull(message = " Lý do khoá tài khoản không được trống ")
    private String reason;
    @NotBlank(message = "vui lòng chọn ngày đến hạn khoá tài khoản")
    private LocalDateTime lockUntil;
}
