package com.hrm.project_spring.scheduler;

import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC11 BR-023: Scheduler tự động purge (xóa cứng) các user đã soft-delete quá 30 ngày.
 * Chạy mỗi ngày lúc 02:00 sáng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPurgeScheduler {

    private final UserRepository userRepository;

    /**
     * Chạy mỗi ngày lúc 02:00 AM.
     * Tìm và xóa cứng tất cả user đã soft-deleted quá 30 ngày.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeDeletedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<User> toDelete = userRepository.findSoftDeletedBefore(threshold);

        if (toDelete.isEmpty()) {
            log.info("UC11-BR-023: Không có user nào cần purge.");
            return;
        }

        log.info("UC11-BR-023: Purge {} user(s) đã soft-deleted quá 30 ngày.", toDelete.size());
        userRepository.deleteAll(toDelete);
        log.info("UC11-BR-023: Đã purge thành công {} user(s).", toDelete.size());
    }
}
