package com.hrm.project_spring.dto.exam;

import com.hrm.project_spring.dto.user.response.UserResponseDto;
import com.hrm.project_spring.enums.ExamStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExamStatus status;
    private LocalDateTime createdAt;
    private UserResponseDto createdBy;
}
