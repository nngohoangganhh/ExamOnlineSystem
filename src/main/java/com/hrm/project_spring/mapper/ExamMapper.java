package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.user.response.UserResponseDto;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.User;

public class ExamMapper {
    public static ExamListResponse toListResponse(Exam exam) {
        if (exam == null) return null;
        return ExamListResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .startTime(exam.getStartDate()) // map to startTime field in DTO
                .endTime(exam.getEndDate())     // map to endTime field in DTO
                .status(exam.getStatus() != null ? exam.getStatus().name() : null)
                .build();
    }

    public static ExamDetailResponse toDetailResponse(Exam exam) {
        if (exam == null) return null;
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .description(exam.getDescription())
                .status(exam.getStatus())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .createdAt(exam.getCreatedAt())
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