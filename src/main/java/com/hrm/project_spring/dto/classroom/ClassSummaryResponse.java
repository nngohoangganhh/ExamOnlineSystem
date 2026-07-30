package com.hrm.project_spring.dto.classroom;


import com.hrm.project_spring.entity.ClassRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ClassSummaryResponse {

    private Long id;
    private String classCode;

    public static ClassSummaryResponse from(ClassRoom classRoom) {
        if (classRoom == null) {
            return null;
        }
        return ClassSummaryResponse.builder()
                .id(classRoom.getId())
                .classCode(classRoom.getCode())
                .build();
    }
}
