package com.hrm.project_spring.dto.exam;

import com.hrm.project_spring.dto.user.response.UserResponseDto;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {
    private Long id;
    private String name;
    private String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalTime createdAt;
    private UserResponseDto createdBy;
  //  private List<UserResponseDto> students;

}
