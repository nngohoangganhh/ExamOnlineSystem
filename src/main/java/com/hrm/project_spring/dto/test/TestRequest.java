package com.hrm.project_spring.dto.test;

import com.hrm.project_spring.enums.ScoringPolicy;
import com.hrm.project_spring.enums.TestType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SRS v1.0 UC27: Tạo bài thi.
 * SRS v1.0 UC28: Cấu hình bài thi.
 * SRS v1.0 UC31: Lịch mở / đóng bài thi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {

    // ─── UC27: Thông tin cơ bản bài thi ───────────────────────────────────────

    /** Kỳ thi chứa bài thi này (bắt buộc). */
    private Long examId;

    /** UC27: Tên bài thi — 5-100 ký tự. */
    @NotBlank(message = "Tên bài thi không được để trống.")
    @Size(min = 5, max = 100, message = "Tên bài thi 5-100 ký tự.")
    private String title;

    /** UC27: Loại bài thi — MAIN (chính thức) | SUPPLEMENTARY (phụ khảo). */
    @NotNull(message = "Loại bài thi không hợp lệ.")
    private TestType type;

    /** UC27 / UC28: Thời lượng làm bài — 1 đến 300 phút (tối đa 5 tiếng). */
    @NotNull(message = "Thời lượng 1-300 phút.")
    @Min(value = 1, message = "Thời lượng 1-300 phút.")
    @Max(value = 300, message = "Thời lượng 1-300 phút.")
    private Integer durationMinutes;

    /** UC27: Điểm tối đa — 1.00 đến 1000.00. */
    @NotNull(message = "Điểm tối đa 1-1000.")
    @DecimalMin(value = "1.00", message = "Điểm tối đa 1-1000.")
    @DecimalMax(value = "1000.00", message = "Điểm tối đa 1-1000.")
    private BigDecimal totalScore;

    // ─── UC28: Cấu hình bài thi ───────────────────────────────────────────────

    /**
     * UC28 / BR-049: Điểm đạt — 0 đến maxScore.
     * Mặc định = 50% totalScore nếu không cấu hình.
     */
    @DecimalMin(value = "0", message = "Điểm đậu không hợp lệ.")
    private BigDecimal passingScore;

    /** UC27 / UC28: Số lần thi tối đa — 1 đến 5. */
    @NotNull(message = "Số lần thi 1-5.")
    @Min(value = 1, message = "Số lần thi 1-5.")
    @Max(value = 5, message = "Số lần thi 1-5.")
    @Builder.Default
    private Integer maxAttempts = 1;

    /**
     * UC28 / BR-046: Thời gian chờ giữa các lượt thi (phút).
     * Bắt buộc nếu maxAttempts > 1. 0-10080 (tối đa 7 ngày).
     */
    @Min(value = 0, message = "Thời gian chờ 0-10080 phút.")
    @Max(value = 10080, message = "Thời gian chờ 0-10080 phút.")
    @Builder.Default
    private Integer cooldownMinutes = 0;

    /**
     * UC27 / BR-050: Chính sách tính điểm khi có nhiều lượt.
     * HIGHEST | LAST | AVERAGE. Chỉ áp dụng khi maxAttempts > 1.
     */
    @NotNull(message = "Chính sách tính điểm không hợp lệ.")
    @Builder.Default
    private ScoringPolicy scoringPolicy = ScoringPolicy.HIGHEST;

    /** UC28 / BR-045: Xáo trộn thứ tự câu hỏi. Mặc định true. */
    @Builder.Default
    private Boolean shuffleQuestions = true;

    /** UC28 / BR-045: Xáo trộn thứ tự đáp án. Mặc định true. */
    @Builder.Default
    private Boolean shuffleOptions = true;

    /** UC28 / UC39: Hiển thị kết quả ngay sau khi nộp. Mặc định false. */
    @Builder.Default
    private Boolean showResultImmediately = false;

    /** UC28 / UC41: Cho phép xem lại bài sau khi nộp. Mặc định true. */
    @Builder.Default
    private Boolean allowReviewAfterSubmit = true;

    /** UC28 / UC41: Hiển thị đáp án đúng khi xem lại. Mặc định false. */
    @Builder.Default
    private Boolean showCorrectAnswers = false;

    /**
     * UC28 / BR-047: Cấu hình chống gian lận dạng JSON.
     * VD: {"fullScreenRequired":true,"blockTabSwitch":true,"maxTabSwitches":3,"blockCopyPaste":true}
     */
    private String antiCheatConfig;

    // ─── UC31: Lịch mở / đóng bài thi ────────────────────────────────────────

    /** UC31: Thời điểm mở bài thi. null = chưa đặt lịch (mở thủ công). */
    private LocalDateTime openTime;

    /** UC31: Thời điểm đóng bài thi. null = không tự đóng. */
    private LocalDateTime closeTime;
}

