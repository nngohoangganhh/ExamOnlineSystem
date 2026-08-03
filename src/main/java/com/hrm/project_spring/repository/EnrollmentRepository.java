package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Enrollment;
import com.hrm.project_spring.entity.Test;
import com.hrm.project_spring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByTestAndUser(Test test, User user);

    boolean existsByTestAndUser(Test test, User user);

    List<Enrollment> findAllByTest(Test test);

    List<Enrollment> findAllByUser(User user);

    @Query("SELECT e FROM Enrollment e WHERE e.test.id = :testId AND e.user.id = :userId")
    Optional<Enrollment> findByTestIdAndUserId(@Param("testId") Long testId,
                                               @Param("userId") Long userId);
}
