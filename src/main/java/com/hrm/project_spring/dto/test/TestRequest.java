package com.hrm.project_spring.dto.test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRequest {

    private Long examId;
    @NotBlank(message = "Title không được để trống")
    private String title;
    @NotNull
    private Integer durationMinutes;
    @NotNull
    private Integer totalScore;

    private LocalTime createAt;


}
