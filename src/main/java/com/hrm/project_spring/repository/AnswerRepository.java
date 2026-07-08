package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionId(Long questionId);
    Optional<Answer> findByIdAndQuestionId(Long answerId, Long questionId);
}
