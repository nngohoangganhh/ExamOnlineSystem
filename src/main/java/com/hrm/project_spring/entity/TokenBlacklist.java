package com.hrm.project_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lưu access token đã bị blacklist sau khi đăng xuất.
 * Cleanup hàng ngày bởi TokenBlacklistCleanupScheduler.
 */
@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "idx_token_blacklist_token", columnList = "token", unique = true),
        @Index(name = "idx_token_blacklist_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
