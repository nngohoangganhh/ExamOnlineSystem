package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    boolean existsBySubjectIdAndCode(Long subjectId, String code);
    boolean existsBySubjectIdAndCodeAndIdNot(Long subjectId, String code, Long id);
    List<Chapter> findBySubjectIdOrderByOrderNumAsc(Long subjectId);
}
