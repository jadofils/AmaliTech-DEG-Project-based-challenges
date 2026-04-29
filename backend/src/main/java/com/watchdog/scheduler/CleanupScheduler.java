package com.watchdog.scheduler;

import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final MonitorRepository monitorRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupDownMonitors() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<?> expired = monitorRepository.findByStatusAndUpdatedAtBefore(MonitorStatus.DOWN, cutoff);
        if (!expired.isEmpty()) {
            monitorRepository.deleteAll((Iterable) expired);
            log.info("Cleanup: removed {} expired DOWN monitors", expired.size());
        }
    }
}
