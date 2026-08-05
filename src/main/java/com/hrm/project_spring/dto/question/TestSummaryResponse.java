package com.hrm.project_spring.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSummaryResponse {
    private Long id;
    private Long examId;
    private String title;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime createAt;
}
