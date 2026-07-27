package com.hrm.project_spring.dto.subject;

import com.hrm.project_spring.entity.Subject;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SubjectSummaryResponse {

    private Long id;
    private String name;

    public static SubjectSummaryResponse from(Subject subject){
        if (subject == null) return null;
        return SubjectSummaryResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .build();
    }
}