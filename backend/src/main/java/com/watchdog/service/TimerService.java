package com.watchdog.service;

import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.MonitorRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimerService {

    private final MonitorRepository monitorRepository;
    private final AlertService alertService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentMap<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final AtomicLong timerCount = new AtomicLong(0);

    public void startTimer(String deviceId, int timeoutSeconds) {
        cancelTimer(deviceId);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            log.warn("Timer expired for device: {}", deviceId);
            processExpiredMonitor(deviceId);
            activeTimers.remove(deviceId);
            timerCount.decrementAndGet();
        }, timeoutSeconds, TimeUnit.SECONDS);
        activeTimers.put(deviceId, future);
        timerCount.incrementAndGet();
        log.debug("Timer started for device: {} ({}s)", deviceId, timeoutSeconds);
    }

    public void resetTimer(String deviceId, int timeoutSeconds) {
        startTimer(deviceId, timeoutSeconds);
        log.debug("Timer reset for device: {}", deviceId);
    }

    public void cancelTimer(String deviceId) {
        ScheduledFuture<?> future = activeTimers.remove(deviceId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            timerCount.decrementAndGet();
            log.debug("Timer cancelled for device: {}", deviceId);
        }
    }

    public long getTimeRemaining(String deviceId) {
        ScheduledFuture<?> future = activeTimers.get(deviceId);
        if (future == null || future.isDone()) return 0;
        return future.getDelay(TimeUnit.SECONDS);
    }

    public boolean isTimerActive(String deviceId) {
        return activeTimers.containsKey(deviceId);
    }

    public long getActiveTimerCount() {
        return timerCount.get();
    }

    private void processExpiredMonitor(String deviceId) {
        monitorRepository.findById(deviceId).ifPresent(monitor -> {
            if (monitor.getStatus() == MonitorStatus.ACTIVE) {
                monitor.setStatus(MonitorStatus.DOWN);
                monitor.setAlertCount(monitor.getAlertCount() + 1);
                monitor.setAlertTriggeredAt(LocalDateTime.now());
                monitorRepository.save(monitor);
                alertService.sendAlert(monitor);
                log.error("Device {} is DOWN", deviceId);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TimerService. Active timers: {}", activeTimers.size());
        activeTimers.keySet().forEach(this::cancelTimer);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
