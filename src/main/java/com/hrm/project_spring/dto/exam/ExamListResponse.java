package com.hrm.project_spring.dto.exam;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * UC25: Response tóm tắt kỳ thi dùng cho danh sách (GET /api/exams).
 */
@Data
@Builder
public class ExamListResponse {
    private Long id;
    private String code;
    private String name;
    private String semester;
    private String academicYear;
    private Long subjectId;
    private String subjectName;
    private LocalDate startTime;
    private LocalDate endTime;
    private String status;
}
