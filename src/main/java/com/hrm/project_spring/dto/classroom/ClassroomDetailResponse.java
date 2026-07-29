package com.hrm.project_spring.dto.classroom;

import com.hrm.project_spring.dto.student.StudentAllResponse;
import com.hrm.project_spring.enums.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomDetailResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String academicYear;
    private LocalDateTime createdAt;
    private ClassStatus classStatus;
    private TeacherSummaryResponse teacherSummary;
    private int studentCount;
    private List<StudentAllResponse> students;
}
