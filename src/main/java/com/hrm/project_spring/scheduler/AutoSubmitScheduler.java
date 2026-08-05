package com.hrm.project_spring.scheduler;

import com.hrm.project_spring.repository.AttemptRepository;
import com.hrm.project_spring.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC40: Tự động nộp bài khi hết giờ.
 * Scheduler chạy mỗi phút, tìm tất cả attempt IN_PROGRESS đã quá scheduledEndAt.
 * Gọi ParticipationService.autoSubmit() cho từng attempt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoSubmitScheduler {

    private final AttemptRepository attemptRepository;
    private final ParticipationService participationService;

    /**
     * Chạy mỗi phút: cron "0 * * * * *"
     * Không dùng fixedRate để tránh chồng lấn khi xử lý lâu.
     */
    @Scheduled(cron = "0 * * * * *")
    public void autoSubmitExpiredAttempts() {
        // Tìm tất cả attempt IN_PROGRESS đã quá giờ (trừ 5s buffer xử lý)
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(5);
        List<com.hrm.project_spring.entity.Attempt> expired =
                attemptRepository.findExpiredAttempts(cutoff);

        if (expired.isEmpty()) return;

        log.info("UC40: AutoSubmitScheduler tìm thấy {} attempt hết hạn.", expired.size());

        for (com.hrm.project_spring.entity.Attempt attempt : expired) {
            try {
                participationService.autoSubmit(attempt.getId());
                log.info("UC40: Auto-submitted attempt {}", attempt.getId());
            } catch (Exception e) {
                // Log và tiếp tục — không để 1 lỗi làm sập toàn bộ batch
                log.error("UC40: Lỗi auto-submit attempt {}: {}", attempt.getId(), e.getMessage());
            }
        }
    }
}
