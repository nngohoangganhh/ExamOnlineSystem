package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.user.response.UserResponseDto;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.Subject;
import com.hrm.project_spring.entity.User;

public class ExamMapper {

    public static ExamListResponse toListResponse(Exam exam) {
        if (exam == null) return null;
        Subject subject = exam.getSubject();
        return ExamListResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .name(exam.getName())
                .semester(exam.getSemester())
                .academicYear(exam.getAcademicYear())
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .startTime(exam.getStartDate())
                .endTime(exam.getEndDate())
                .status(exam.getStatus() != null ? exam.getStatus().name() : null)
                .build();
    }

    public static ExamDetailResponse toDetailResponse(Exam exam) {
        if (exam == null) return null;
        Subject subject = exam.getSubject();
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .code(exam.getCode())
                .name(exam.getName())
                .description(exam.getDescription())
                .semester(exam.getSemester())
                .academicYear(exam.getAcademicYear())
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .status(exam.getStatus())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .deletedAt(exam.getDeletedAt())
                .createdBy(toUser(exam.getCreatedBy()))
                .build();
    }

    private static UserResponseDto toUser(User user) {
        if (user == null) return null;
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}