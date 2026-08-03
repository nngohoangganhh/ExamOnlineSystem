package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.enums.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>,
        JpaSpecificationExecutor<Question> {

    boolean existsBySubjectId(Long subjectId);

    boolean existsByChapterId(Long chapterId);

    /** UC18: Tìm câu hỏi chưa bị xóa. */
    @Query("SELECT q FROM Question q WHERE q.id = :id AND q.deletedAt IS NULL")
    java.util.Optional<Question> findActiveById(@Param("id") Long id);

    /** UC24: Đếm câu hỏi theo trạng thái (dùng cho dashboard). */
    long countByStatus(QuestionStatus status);

    /** UC24: Đếm câu hỏi chưa xóa. */
    long countByDeletedAtIsNull();

    /** Batch approve: lấy danh sách câu hỏi PENDING_REVIEW để duyệt. */
    List<Question> findAllByStatusAndIdIn(QuestionStatus status, List<Long> ids);

    /** UC18: Soft delete theo id. */
    @Modifying
    @Query("UPDATE Question q SET q.deletedAt = :now, q.status = 'ARCHIVED' WHERE q.id = :id")
    void softDelete(@Param("id") Long id, @Param("now") LocalDateTime now);
}
