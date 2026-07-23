package com.hrm.project_spring.scheduler;

import com.hrm.project_spring.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler tự động xóa các access token đã hết hạn khỏi blacklist.
 * Chạy mỗi ngày lúc 03:00 AM để tránh bảng token_blacklist phình to vô hạn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistCleanupScheduler {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    /**
     * Chạy mỗi ngày lúc 03:00 AM.
     * Xóa các token đã hết hạn tự nhiên (không còn có thể bị lạm dụng).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        log.info("TokenBlacklist: Bắt đầu cleanup token hết hạn trước {}", now);
        tokenBlacklistRepository.deleteByExpiresAtBefore(now);
        log.info("TokenBlacklist: Cleanup hoàn tất.");
    }
}
