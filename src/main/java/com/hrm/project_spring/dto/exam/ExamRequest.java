package com.hrm.project_spring.dto.exam;

import com.hrm.project_spring.enums.ExamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRequest {
    private String code;
    private String name;
    private String description;
    private String semester;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExamStatus status;
}
