package com.watchdog.service;

import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.MonitorRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerService {

    private final MonitorRepository monitorRepository;
    private final AlertService alertService;

    @Value("${timer.thread.pool.size:10}")
    private int threadPoolSize;

    @Value("${timer.shutdown.timeout.seconds:30}")
    private int shutdownTimeout;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public void startTimer(String id, int timeoutSeconds) {
        cancelTimer(id);
        ScheduledFuture<?> future = scheduler.schedule(
                () -> processExpiredMonitor(id),
                timeoutSeconds,
                TimeUnit.SECONDS
        );
        timers.put(id, future);
        log.debug("Timer started for device: {} ({}s)", id, timeoutSeconds);
    }

    public void resetTimer(String id, int timeoutSeconds) {
        startTimer(id, timeoutSeconds);
        log.debug("Timer reset for device: {}", id);
    }

    public void cancelTimer(String id) {
        ScheduledFuture<?> existing = timers.remove(id);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
            log.debug("Timer cancelled for device: {}", id);
        }
    }

    public long getTimeRemaining(String id, int timeoutSeconds) {
        ScheduledFuture<?> future = timers.get(id);
        if (future == null || future.isDone()) return 0;
        return future.getDelay(TimeUnit.SECONDS);
    }

    public int getActiveTimerCount() {
        return (int) timers.values().stream().filter(f -> !f.isDone()).count();
    }

    private void processExpiredMonitor(String id) {
        monitorRepository.findById(id).ifPresent(monitor -> {
            if (monitor.getStatus() == MonitorStatus.ACTIVE) {
                monitor.setStatus(MonitorStatus.DOWN);
                monitor.setAlertCount(monitor.getAlertCount() + 1);
                monitor.setAlertTriggeredAt(LocalDateTime.now());
                monitorRepository.save(monitor);
                alertService.sendAlert(monitor);
                log.error("Monitor expired: device {} is DOWN", id);
            }
        });
        timers.remove(id);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
