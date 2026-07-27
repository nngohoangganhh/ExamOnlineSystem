package com.hrm.project_spring.dto.chapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String name;
    private String code;
    private Integer order;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
