package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    // Lịch sử thi của 1 user (cho student xem lịch sử của mình)
    Page<ExamAttempt> findByUserId(Long userId, Pageable pageable);

    // Tất cả attempts của 1 test (cho admin/teacher xem)
    Page<ExamAttempt> findByTestId(Long testId, Pageable pageable);

    // Tìm result theo id và đảm bảo thuộc về user đó (bảo mật)
    Optional<ExamAttempt> findByIdAndUserId(Long attemptId, Long userId);

    // Tìm result của 1 test thuộc về 1 user (kiểm tra đã thi chưa)
    // CHÚ Ý: Dùng findFirst để tránh NonUniqueResultException khi có nhiều attempts
    Optional<ExamAttempt> findFirstByUserIdAndTestIdOrderByIdDesc(Long userId, Long testId);


    // Kiểm tra student đã nộp bài chưa (submitTime != null)
    boolean existsByUserIdAndTestIdAndSubmitTimeIsNotNull(Long userId, Long testId);

    // Tất cả attempts của 1 exam (join qua test)
    @Query("SELECT a FROM ExamAttempt a WHERE a.test.exam.id = :examId")
    Page<ExamAttempt> findByExamId(@Param("examId") Long examId, Pageable pageable);

    // Tất cả attempts của 1 user + 1 exam
    @Query("SELECT a FROM ExamAttempt a WHERE a.user.id = :userId AND a.test.exam.id = :examId")
    Page<ExamAttempt> findByUserIdAndExamId(@Param("userId") Long userId,
                                             @Param("examId") Long examId,
                                             Pageable pageable);

    // Đếm số lần thi đã nộp của 1 user (student dashboard)
    long countByUserIdAndSubmitTimeIsNotNull(Long userId);

    // Đếm tổng số lần thi (kể cả chưa nộp) của 1 user
    long countByUserId(Long userId);

    // Tính điểm trung bình của 1 user (student dashboard)
    @Query("SELECT AVG(a.score) FROM ExamAttempt a WHERE a.user.id = :userId AND a.submitTime IS NOT NULL")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    /**
     * UC11: Kiểm tra user có đang thi dở (startTime != null, submitTime == null) không.
     * Nếu có → không cho phép xóa user.
     */
    boolean existsByUserIdAndSubmitTimeIsNull(Long userId);
}
