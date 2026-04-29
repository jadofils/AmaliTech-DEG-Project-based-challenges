package com.watchdog.scheduler;

import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.AuditLogRepository;
import com.watchdog.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final MonitorRepository monitorRepository;
    private final AuditLogRepository auditLogRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldMonitors() {
        log.info("Starting cleanup of old monitors");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        int deletedCount = monitorRepository.deleteOldMonitors(MonitorStatus.DOWN, cutoffTime);
        if (deletedCount > 0) {
            log.info("Cleaned up {} down monitors older than 24 hours", deletedCount);
        } else {
            log.debug("No old monitors to clean up");
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = auditLogRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleanup: removed {} audit log entries older than 30 days", deleted);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void logMetrics() {
        if (log.isDebugEnabled()) {
            long totalCount = monitorRepository.count();
            long activeCount = monitorRepository.findByStatus(MonitorStatus.ACTIVE, Pageable.unpaged()).getTotalElements();
            log.debug("Metrics - Total: {}, Active: {}", totalCount, activeCount);
        }
    }
}
