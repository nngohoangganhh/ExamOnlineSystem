package com.hrm.project_spring.mapper;


import com.hrm.project_spring.dto.enrollment.EnrollmentResponse;
import com.hrm.project_spring.entity.Enrollment;

public class EnrollmentMapper {

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                new EnrollmentResponse.StudentInfo(
                        enrollment.getUser().getId(),
                        enrollment.getUser().getUsername(),
                        enrollment.getUser().getFullName(),
                        enrollment.getUser().getEmail(),
                        enrollment.getUser().getStudentCode(),
                        enrollment.getUser().getStatus().name()
                ),
                enrollment.getAttemptsUsed(),
                enrollment.getAssignedAt(),
                new EnrollmentResponse.AssignerInfo(
                        enrollment.getAssignedBy().getId(),
                        enrollment.getAssignedBy().getFullName(),
                        enrollment.getAssignedBy().getUsername()
                )
        );
    }
}
