package com.hrm.project_spring.dto.chapter;

import com.hrm.project_spring.entity.Chapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterSummaryResponse {
    private Long id;
    private String name;

    public static ChapterSummaryResponse from(Chapter chapter) {
        if (chapter == null) return null;
        return ChapterSummaryResponse.builder()
                .id(chapter.getId())
                .name(chapter.getName())
                .build();
    }
}
