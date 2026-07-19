package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.user.UserResponseDto;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.User;

import java.time.LocalDate;

public class ExamMapper {
    public static ExamListResponse toListResponse(Exam exam) {
        return ExamListResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .startTime(LocalDate.from(exam.getStartTime()))
                .endTime(LocalDate.from(exam.getEndTime()))
                .status(exam.getStatus())
                .build();
    }

    public static ExamDetailResponse toDetailResponse(Exam exam) {
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .description(exam.getDescription())
                .status(exam.getStatus())
                .startTime(exam.getStartTime().toLocalTime())
                .endTime(exam.getEndTime().toLocalTime())
                .createdAt(exam.getCreatedAt())
                .createdBy(toUser(exam.getCreatedBy()))
//                .students(
//                        exam.getStudents().stream()
//                                .map(ExamMapper::toUser)
//                                .toList()
//                )
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