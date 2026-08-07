package com.hrm.project_spring.dto.exam;

import com.hrm.project_spring.dto.user.response.UserResponseDto;
import com.hrm.project_spring.enums.ExamStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UC25: Response chi tiết kỳ thi — trả về sau khi tạo hoặc lấy chi tiết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {
    private Long id;

    /** UC25: Mã kỳ thi (unique). */
    private String code;

    private String name;
    private String description;

    /** UC25: Học kỳ — "1", "2", "3". */
    private String semester;

    /** UC25: Năm học — "YYYY-YYYY". */
    private String academicYear;

    /** UC25: ID môn học liên kết. */
    private Long subjectId;

    /** UC25: Tên môn học liên kết (để FE hiển thị, không cần gọi thêm API). */
    private String subjectName;

    private LocalDate startDate;
    private LocalDate endDate;
    private ExamStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** UC26: Thời điểm soft-delete (null nếu chưa xóa). */
    private LocalDateTime deletedAt;

    private UserResponseDto createdBy;
}


