package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    boolean existsBySubjectId(Long subjectId);
    boolean existsByChapterId(Long chapterId);
}
