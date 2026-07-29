package com.hrm.project_spring.dto.classroom;

import com.hrm.project_spring.enums.ClassStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassRoomResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String academicYear;
    private LocalDateTime createdAt;
    private String teacherName;
    private int studentCount;
    private ClassStatus status;
}
