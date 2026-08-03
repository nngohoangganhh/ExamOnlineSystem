package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Attempt;
import com.hrm.project_spring.entity.AttemptAnswer;
import com.hrm.project_spring.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    Optional<AttemptAnswer> findByAttemptAndQuestion(Attempt attempt, Question question);

    List<AttemptAnswer> findAllByAttempt(Attempt attempt);

    List<AttemptAnswer> findAllByAttemptOrderByIdAsc(Attempt attempt);
}
