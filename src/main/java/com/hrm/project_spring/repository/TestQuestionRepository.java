package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Test;
import com.hrm.project_spring.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {

    List<TestQuestion> findAllByTestOrderByOrderNumAsc(Test test);

    boolean existsByQuestion_Id(Long questionId);

    /** UC18: Kiểm tra câu hỏi đang được dùng trong bài thi nào đó. */
    boolean existsByTest_IdAndQuestion_Id(Long testId, Long questionId);

    int countByTest(Test test);

    @Modifying
    @Query("DELETE FROM TestQuestion tq WHERE tq.test.id = :testId")
    void deleteAllByTestId(@Param("testId") Long testId);
}
