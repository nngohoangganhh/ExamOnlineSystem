package com.hrm.project_spring.dto.question;

import com.hrm.project_spring.dto.chapter.ChapterSummaryResponse;
import com.hrm.project_spring.dto.subject.SubjectSummaryResponse;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailResponse {

    private Long id;

    private String stem;

    private QuestionType type;

    private Integer bloomLevel;

    private BigDecimal score;

    private QuestionStatus status;

    private SubjectSummaryResponse subject;

    private ChapterSummaryResponse chapter;

    private List<QuestionOptionResponse> options;

    private List<String> tags;

    private String explanation;

    private String referenceAnswer;

    private String rubric;

    private String createdBy;

    private LocalDateTime createdAt;
}
