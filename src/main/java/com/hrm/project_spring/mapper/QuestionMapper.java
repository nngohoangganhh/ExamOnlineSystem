package com.hrm.project_spring.mapper;

import com.hrm.project_spring.dto.chapter.ChapterSummaryResponse;
import com.hrm.project_spring.dto.question.QuestionDetailResponse;
import com.hrm.project_spring.dto.question.QuestionOptionResponse;
import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.dto.subject.SubjectSummaryResponse;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.Tag;

public class QuestionMapper {

    public static QuestionResponse toResponse(Question question) {
        if (question == null) return null;
        return QuestionResponse.builder()
                .id(question.getId())
                .stem(question.getStem())
                .status(question.getStatus())
                .type(question.getType())
                .bloomLevel(question.getBloomLevel())
                .chapterName(question.getChapter() != null ? question.getChapter().getName() : null)
                .subjectName(question.getSubject() != null ? question.getSubject().getName() : null)
                .createdByName(question.getCreatedBy().getUsername())
                .createdAt(question.getCreatedAt())

                .build();
    }

    public static QuestionDetailResponse toMapperResponse(Question question) {
        if (question == null) return null;
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .stem(question.getStem())
                .type(question.getType())
                .bloomLevel(question.getBloomLevel())
                .score(question.getScore())
                .status(question.getStatus())

                .subject(SubjectSummaryResponse.from(question.getSubject()))
                .chapter(ChapterSummaryResponse.from(question.getChapter()))

                .options(
                        question.getQuestionOptions() == null ? null :
                        question.getQuestionOptions()
                                .stream()
                                .map(QuestionOptionResponse::from)
                                 .toList()
                )

                .tags(
                        question.getTags() == null ? null :
                        question.getTags()
                                .stream()
                                .map(Tag::getName)
                                .toList()
                )

                .explanation(question.getExplanation())
                .referenceAnswer(question.getReferenceAnswer())
                .rubric(question.getRubric())

                .createdBy(
                        question.getCreatedBy() == null
                                ? null
                                : question.getCreatedBy().toString()
                )

                .createdAt(question.getCreatedAt())

                .build();
    }
}
