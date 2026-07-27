package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    // Load toàn bộ câu trả lời của 1 result (fetch eager để tránh N+1)
    @Query("SELECT sa FROM StudentAnswer sa " +
            "JOIN FETCH sa.question q " +
            "LEFT JOIN FETCH sa.selectedAnswer " +
            "WHERE sa.attempt.id = :attemptId")
    List<StudentAnswer> findByAttemptIdWithDetails(@Param("attemptId") Long attemptId);
}

