package com.hrm.project_spring.dto.test;

import com.hrm.project_spring.enums.TestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSummaryResponse {
    private Long id;
    private Long examId;
    private String examName;
    private String title;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private TestStatus status;
    private LocalDateTime createdAt;
}
