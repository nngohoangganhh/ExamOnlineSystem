package com.hrm.project_spring.dto.question;


import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String stem;
    private QuestionType type;
    private Integer bloomLevel;
    private QuestionStatus status;
    private String subjectName;
    private String chapterName;
    private String createdByName;
    private LocalDateTime createdAt;
}
