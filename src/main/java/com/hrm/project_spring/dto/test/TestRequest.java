package com.hrm.project_spring.dto.test;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request body để tạo mới hoặc cập nhật bài thi (Test).
 * UC27, UC28.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {

    /** ID kỳ thi cha. Bắt buộc khi tạo bài thi. */
    @NotNull(message = "Kỳ thi (examId) không được để trống")
    private Long examId;

    @NotBlank(message = "Tiêu đề bài thi không được để trống")
    @Size(min = 3, max = 200, message = "Tiêu đề bài thi từ 3 đến 200 ký tự")
    private String title;

    /** Thời gian làm bài (phút). */
    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài tối thiểu 1 phút")
    @Max(value = 600, message = "Thời gian làm bài tối đa 600 phút")
    private Integer durationMinutes;

    /** Tổng điểm tối đa. Nếu null, hệ thống tự tính từ tổng điểm các câu. */
    @DecimalMin(value = "0.01", message = "Tổng điểm phải lớn hơn 0")
    @DecimalMax(value = "1000", message = "Tổng điểm tối đa 1000")
    private Double totalScore;
}
