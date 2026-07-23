package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * UC07: Bản ghi audit log cho mọi hành động quan trọng trong hệ thống.
 * Immutable sau khi tạo — không có setter để bảo vệ tính toàn vẹn (BR-016).
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID của user thực hiện hành động (null nếu là hệ thống hoặc anonymous) */
    @Column(name = "user_id")
    private Long userId;

    /** Username tại thời điểm thực hiện (lưu snapshot, không foreign key) */
    @Column(name = "username", length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    /** Địa chỉ IP của request */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** User-Agent của browser/client */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * Thông tin bổ sung dạng JSON:
     * - targetId: ID của đối tượng bị tác động
     * - targetUsername: tên user bị tác động
     * - diff: before/after khi update
     * - reason: lý do khi lock/delete
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
