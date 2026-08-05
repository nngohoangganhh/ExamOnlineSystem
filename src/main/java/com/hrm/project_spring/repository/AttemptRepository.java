package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Attempt;
import com.hrm.project_spring.entity.Test;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findAllByTestAndUser(Test test, User user);

    /** Đếm số lượt đã dùng của 1 thí sinh trong 1 bài thi. */
    int countByTestAndUser(Test test, User user);

    /** Lượt làm bài đang IN_PROGRESS của thí sinh. */
    Optional<Attempt> findByTestAndUserAndStatus(Test test, User user, AttemptStatus status);

    /** UC40: Tìm các attempt đang IN_PROGRESS đã quá scheduledEndAt (dùng cho auto-submit). */
    @Query("""
            SELECT a FROM Attempt a
            WHERE a.status = 'IN_PROGRESS'
              AND a.scheduledEndAt < :cutoff
            """)
    List<Attempt> findExpiredAttempts(@Param("cutoff") LocalDateTime cutoff);

    List<Attempt> findAllByTestOrderByUserAscAttemptNumberAsc(Test test);

    long countByUserId(Long userId);

    long countByUserIdAndSubmittedAtIsNotNull(Long userId);

    @Query("SELECT AVG(a.finalScore) FROM Attempt a WHERE a.user.id = :userId AND a.finalScore IS NOT NULL")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndTestIdAndSubmittedAtIsNotNull(Long userId, Long testId);

    Optional<Attempt> findFirstByUserIdAndTestIdOrderByIdDesc(Long userId, Long testId);
}

