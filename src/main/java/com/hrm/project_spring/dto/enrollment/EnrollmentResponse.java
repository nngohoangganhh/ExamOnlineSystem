package com.hrm.project_spring.dto.enrollment;


import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        StudentInfo user,
        Integer attemptsUsed,
        LocalDateTime assignedAt,
        AssignerInfo assignedBy
) {
    public record StudentInfo(
            Long id,
            String username,
            String fullName,
            String email,
            String studentCode,
            String status
    ) {}

    public record AssignerInfo(
            Long id,
            String fullName,
            String username
    ) {}
}