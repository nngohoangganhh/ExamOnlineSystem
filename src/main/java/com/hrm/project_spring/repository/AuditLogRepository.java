package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.AuditLog;
import com.hrm.project_spring.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * UC07: Tìm kiếm audit log với nhiều bộ lọc kết hợp.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.userId = :userId)
            AND (:action IS NULL OR a.action = :action)
            AND (:ip IS NULL OR a.ipAddress = :ip)
            AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
            AND (:toDate IS NULL OR a.createdAt <= :toDate)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("userId") Long userId,
            @Param("action") AuditAction action,
            @Param("ip") String ip,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    /**
     * UC07: Export audit log (không phân trang, giới hạn 10.000 dòng).
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.userId = :userId)
            AND (:action IS NULL OR a.action = :action)
            AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
            AND (:toDate IS NULL OR a.createdAt <= :toDate)
            ORDER BY a.createdAt DESC
            """)
    java.util.List<AuditLog> findForExport(
            @Param("userId") Long userId,
            @Param("action") AuditAction action,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
