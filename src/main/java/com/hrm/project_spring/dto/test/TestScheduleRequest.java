package com.hrm.project_spring.dto.test;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SRS v1.0 UC31: Request cấu hình lịch mở / đóng bài thi.
 * BR-053: Timezone mặc định Asia/Ho_Chi_Minh (UTC+7).
 * BR-054: Grace period 60 giây sau closeTime trước khi auto-submit.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestScheduleRequest {

    /**
     * UC31: Thời điểm mở bài thi — ISO 8601, không quá now + 365 ngày.
     * null khi dùng openNow = true.
     */
    private LocalDateTime openTime;

    /**
     * UC31: Thời điểm đóng bài thi — phải sau openTime, không quá openTime + 30 ngày.
     * null khi dùng openNow = true và không muốn tự đóng.
     */
    private LocalDateTime closeTime;

    /**
     * UC31 / BR-053: Timezone — IANA format (vd: Asia/Ho_Chi_Minh).
     * Mặc định theo profile của Teacher nếu không cấu hình.
     */
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    /**
     * UC31: Nếu true → mở bài thi ngay lập tức (status = OPEN),
     * bỏ qua openTime / closeTime.
     */
    @Builder.Default
    private boolean openNow = false;
}
