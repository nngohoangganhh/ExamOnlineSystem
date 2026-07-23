package com.hrm.project_spring.service;

import com.hrm.project_spring.entity.AuditLog;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC07: Dịch vụ ghi và truy vấn Audit Log.
 * - log() chạy trong transaction RIÊNG (Propagation.REQUIRES_NEW)
 *   để đảm bảo audit log được ghi kể cả khi transaction chính rollback.
 * - BR-015: Lưu tối thiểu 12 tháng (không xóa tự động).
 * - BR-016: Immutable — entity không có setter, chỉ tạo mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AuditLogRepository auditLogRepository;

    /**
     * Ghi audit log — an toàn, không ném exception.
     *
     * @param userId    ID user thực hiện (null = anonymous/system)
     * @param username  Username tại thời điểm thực hiện
     * @param action    Loại hành động
     * @param request   HttpServletRequest để lấy IP và User-Agent
     * @param details   JSON string mô tả chi tiết (diff, targetId, reason…)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String username, AuditAction action,
                    HttpServletRequest request, String details) {
        try {
            String ip = extractIp(request);
            String userAgent = request != null
                    ? truncate(request.getHeader("User-Agent"), 512)
                    : null;

            auditLogRepository.save(AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .details(details)
                    .build());
        } catch (Exception e) {
            // Audit log không được làm sập luồng chính
            log.error("UC07: Không thể ghi audit log [action={}]: {}", action, e.getMessage());
        }
    }

    /**
     * Overload không có HttpServletRequest (dùng cho scheduler, batch job).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String username, AuditAction action, String details) {
        log(userId, username, action, null, details);
    }

    /**
     * UC07: Tìm kiếm audit log với bộ lọc.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> search(Long userId, AuditAction action, String ip,
                                 LocalDateTime fromDate, LocalDateTime toDate,
                                 int pageNo, int pageSize) {
        return auditLogRepository.search(userId, action, ip, fromDate, toDate,
                PageRequest.of(pageNo, pageSize));
    }

    /**
     * UC07: Export audit log sang CSV (giới hạn 10.000 dòng).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(Long userId, AuditAction action,
                            LocalDateTime fromDate, LocalDateTime toDate) {
        List<AuditLog> logs = auditLogRepository.findForExport(
                userId, action, fromDate, toDate,
                PageRequest.of(0, MAX_EXPORT_ROWS));

        StringBuilder sb = new StringBuilder();
        sb.append("ID,USER_ID,USERNAME,ACTION,IP_ADDRESS,USER_AGENT,DETAILS,CREATED_AT\n");
        for (AuditLog entry : logs) {
            sb.append(csvEscape(String.valueOf(entry.getId()))).append(",");
            sb.append(csvEscape(entry.getUserId() != null ? String.valueOf(entry.getUserId()) : "")).append(",");
            sb.append(csvEscape(entry.getUsername())).append(",");
            sb.append(csvEscape(entry.getAction() != null ? entry.getAction().name() : "")).append(",");
            sb.append(csvEscape(entry.getIpAddress())).append(",");
            sb.append(csvEscape(entry.getUserAgent())).append(",");
            sb.append(csvEscape(entry.getDetails())).append(",");
            sb.append(csvEscape(entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : "")).append("\n");
        }

        log.info("UC07: Export {} audit log entries", logs.size());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ======================== PRIVATE HELPERS ========================

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
