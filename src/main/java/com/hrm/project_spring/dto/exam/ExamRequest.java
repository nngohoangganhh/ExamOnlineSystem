package com.hrm.project_spring.dto.exam;

import com.hrm.project_spring.enums.ExamStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * SRS v1.0 UC25: Request tạo / cập nhật kỳ thi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRequest {

    /** UC25: Mã kỳ thi — duy nhất, 3-30 ký tự alphanumeric + gạch ngang. */
    @NotBlank(message = "Mã kỳ thi không được để trống.")
    @Size(min = 3, max = 30, message = "Mã kỳ thi 3-30 ký tự và không trùng.")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "Mã kỳ thi chỉ chứa chữ cái, số và dấu gạch ngang.")
    private String code;

    /** UC25: Tên kỳ thi — 5-150 ký tự. */
    @NotBlank(message = "Tên kỳ thi không được để trống.")
    @Size(min = 5, max = 150, message = "Tên kỳ thi 5-150 ký tự.")
    private String name;

    /** UC25: Mô tả — tối đa 1000 ký tự. */
    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự.")
    private String description;

    /**
     * UC25: Môn học liên kết — bắt buộc.
     * FK tới bảng subjects.
     */
    @NotNull(message = "Môn học không hợp lệ.")
    private Long subjectId;

    /**
     * UC25: Học kỳ — chỉ nhận "1" (HK1), "2" (HK2), "3" (Hè).
     */
    @NotBlank(message = "Học kỳ không hợp lệ.")
    @Pattern(regexp = "^[123]$", message = "Học kỳ không hợp lệ. Chỉ nhận: 1, 2, 3.")
    private String semester;

    /** UC25: Năm học — định dạng YYYY-YYYY. */
    @NotBlank(message = "Năm học không hợp lệ.")
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Năm học không hợp lệ (định dạng YYYY-YYYY).")
    private String academicYear;

    /** UC25: Ngày bắt đầu kỳ thi. */
    @NotNull(message = "Ngày bắt đầu không hợp lệ.")
    private LocalDate startDate;

    /** UC25: Ngày kết thúc kỳ thi — phải sau startDate. */
    @NotNull(message = "Ngày kết thúc không hợp lệ.")
    private LocalDate endDate;

    /**
     * UC25 A1: true = Save & Publish (status = PUBLISHED).
     * false = Save Draft (mặc định).
     */
    @Builder.Default
    private boolean publish = false;

    /**
     * UC26 A1: Xác nhận sửa kỳ thi đang Published.
     * Bắt buộc = true khi exam đang PUBLISHED và muốn cập nhật.
     */
    @Builder.Default
    private boolean confirm = false;

    /**
     * Chỉ dùng khi cập nhật nội bộ (UC26).
     * Không nhận từ client khi tạo mới.
     */
    private ExamStatus status;
}